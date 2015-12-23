package com.qa.selenium.junit.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;

/**
 * A custom JUnit runner that parallelizes the sets of parameters and within
 * each set supports the parallelization of methods. This runner utilizes the
 * existing JUnit {@link Parameter} and {@link Parameters} annotations.
 * <p>
 * To set the number of concurrent parameter sets to run, set the
 * <code>parallelParameters</code> property. The default value is the number of
 * parameter sets.
 * 
 * <pre>
 * mvn test -Dtest=MyTest -DparallelParameters=3
 * </pre>
 * <p>
 * To set the number of concurrent methods to run for each set of parameters,
 * set the <code>parallelParameterMethods</code> property. The default value is
 * 1.
 * 
 * <pre>
 * mvn test -Dtest=MyTest -DparallelParameterMethods=4
 * </pre>
 * <p>
 * Multiplying the values of each setting will give you the maximum number of
 * tests that will run in the test class at any given time. Referencing the
 * above example, 3 parameters * 4 methods per parameter = 12 maximum concurrent
 * tests.
 * <p>
 * This class is essentially a re-implementation of {@link Parameterized} with
 * the use of a custom scheduler to support parallelization.
 * 
 * @author Matthew DeTullio
 * @author Ross Rowe
 */
public class ParallelParameterized extends Suite {

	private static final List<Runner> NO_RUNNERS = Collections
			.<Runner> emptyList();

	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	private int numParameterSets;

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public ParallelParameterized(Class<?> klass) throws Throwable {
		super(klass, NO_RUNNERS);
		Parameters parameters = getParametersMethod().getAnnotation(
				Parameters.class);
		createRunnersForParameters(allParameters(), parameters.name());

		int numThreads;
		String threads = System.getProperty("parallelParameters");

		if (StringUtils.isNotBlank(threads)) {
			// Use property to set # of threads if present
			numThreads = Integer.parseInt(threads);
		} else {
			// Default # of threads to # of parameters
			numThreads = numParameterSets;
		}

		if (numThreads < 1) {
			numThreads = 1;
		}

		setScheduler(new NonBlockingAsynchronousRunner(numThreads));
	}

	protected List<Runner> getChildren() {
		return runners;
	}

	@SuppressWarnings("unchecked")
	private Iterable<Object[]> allParameters() throws Throwable {
		Object parameters = getParametersMethod().invokeExplosively(null);
		if (parameters instanceof Iterable) {
			return (Iterable<Object[]>) parameters;
		} else {
			throw parametersMethodReturnedWrongType();
		}
	}

	private FrameworkMethod getParametersMethod() throws Exception {
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(
				Parameters.class);
		for (FrameworkMethod each : methods) {
			if (each.isStatic() && each.isPublic()) {
				return each;
			}
		}

		throw new Exception("No public static parameters method on class "
				+ getTestClass().getName());
	}

	private void createRunnersForParameters(Iterable<Object[]> allParameters,
			String namePattern) throws InitializationError, Exception {
		try {
			int i = 0;
			for (Object[] parametersOfSingleTest : allParameters) {
				String name = nameFor(namePattern, i, parametersOfSingleTest);
				TestClassRunnerForParameters runner = new TestClassRunnerForParameters(
						getTestClass().getJavaClass(), parametersOfSingleTest,
						name);
				runners.add(runner);
				++i;
			}
			numParameterSets = i + 1;
		} catch (ClassCastException e) {
			throw parametersMethodReturnedWrongType();
		}
	}

	private String nameFor(String namePattern, int index, Object[] parameters) {
		String finalPattern = namePattern.replaceAll("\\{index\\}",
				Integer.toString(index));
		String name = MessageFormat.format(finalPattern, parameters);
		return "[" + name + "]";
	}

	private Exception parametersMethodReturnedWrongType() throws Exception {
		String className = getTestClass().getName();
		String methodName = getParametersMethod().getName();
		String message = MessageFormat.format(
				"{0}.{1}() must return an Iterable of arrays.", className,
				methodName);
		return new Exception(message);
	}

	private List<FrameworkField> getAnnotatedFieldsByParameter() {
		return getTestClass().getAnnotatedFields(Parameter.class);
	}

	private boolean fieldsAreAnnotated() {
		return !getAnnotatedFieldsByParameter().isEmpty();
	}

	/**
	 * Reimplementation of
	 * {@link org.junit.runners.Parameterized.TestClassRunnerForParameters} that
	 * uses a {@link NonBlockingAsynchronousRunner} to schedule test execution.
	 */
	private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {
		private final Object[] fParameters;

		private final String fName;

		TestClassRunnerForParameters(Class<?> type, Object[] parameters,
				String name) throws InitializationError {
			super(type);
			fParameters = parameters;
			fName = name;

			String threads = System
					.getProperty("parallelParameterMethods", "1");
			int numThreads = Integer.parseInt(threads);
			setScheduler(new NonBlockingAsynchronousRunner(numThreads));
		}

		public Object createTest() throws Exception {
			if (fieldsAreAnnotated()) {
				return createTestUsingFieldInjection();
			} else {
				return createTestUsingConstructorInjection();
			}
		}

		private Object createTestUsingConstructorInjection() throws Exception {
			return getTestClass().getOnlyConstructor().newInstance(fParameters);
		}

		private Object createTestUsingFieldInjection() throws Exception {
			List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
			if (annotatedFieldsByParameter.size() != fParameters.length) {
				throw new Exception(
						"Wrong number of parameters and @Parameter fields."
								+ " @Parameter fields counted: "
								+ annotatedFieldsByParameter.size()
								+ ", available parameters: "
								+ fParameters.length + ".");
			}
			Object testClassInstance = getTestClass().getJavaClass()
					.newInstance();
			for (FrameworkField each : annotatedFieldsByParameter) {
				Field field = each.getField();
				Parameter annotation = field.getAnnotation(Parameter.class);
				int index = annotation.value();
				try {
					field.set(testClassInstance, fParameters[index]);
				} catch (IllegalArgumentException iare) {
					throw new Exception(getTestClass().getName()
							+ ": Trying to set " + field.getName()
							+ " with the value " + fParameters[index]
							+ " that is not the right type ("
							+ fParameters[index].getClass().getSimpleName()
							+ " instead of " + field.getType().getSimpleName()
							+ ").", iare);
				}
			}
			return testClassInstance;
		}

		protected String getName() {
			return fName;
		}

		protected String testName(FrameworkMethod method) {
			return method.getName() + getName();
		}

		protected void validateConstructor(List<Throwable> errors) {
			validateOnlyOneConstructor(errors);
			if (fieldsAreAnnotated()) {
				validateZeroArgConstructor(errors);
			}
		}

		protected void validateFields(List<Throwable> errors) {
			super.validateFields(errors);
			if (fieldsAreAnnotated()) {
				List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
				int[] usedIndices = new int[annotatedFieldsByParameter.size()];
				for (FrameworkField each : annotatedFieldsByParameter) {
					int index = each.getField().getAnnotation(Parameter.class)
							.value();
					if (index < 0
							|| index > annotatedFieldsByParameter.size() - 1) {
						errors.add(new Exception("Invalid @Parameter value: "
								+ index + ". @Parameter fields counted: "
								+ annotatedFieldsByParameter.size()
								+ ". Please use an index between 0 and "
								+ (annotatedFieldsByParameter.size() - 1) + "."));
					} else {
						usedIndices[index]++;
					}
				}
				for (int index = 0; index < usedIndices.length; index++) {
					int numberOfUse = usedIndices[index];
					if (numberOfUse == 0) {
						errors.add(new Exception("@Parameter(" + index
								+ ") is never used."));
					} else if (numberOfUse > 1) {
						errors.add(new Exception("@Parameter(" + index
								+ ") is used more than once (" + numberOfUse
								+ ")."));
					}
				}
			}
		}

		protected Statement classBlock(final RunNotifier notifier) {
			return childrenInvoker(notifier);
		}

		protected Annotation[] getRunnerAnnotations() {
			return new Annotation[0];
		}
	}

	/**
	 * {@link RunnerScheduler} which allows tests to run concurrently. A fixed
	 * thread pool is used to invoke the tests, which are added to a list of
	 * {@link Future}s.
	 * <p>
	 * Kanged from SauceLabs' ConcurrentParameterized.
	 * 
	 * @author Ross Rowe
	 */
	private static class NonBlockingAsynchronousRunner implements
			RunnerScheduler {
		private final List<Future<Object>> futures = Collections
				.synchronizedList(new ArrayList<Future<Object>>());
		private final ExecutorService fService;

		public NonBlockingAsynchronousRunner(int numThreads) {
			fService = Executors.newFixedThreadPool(numThreads);
		}

		public void schedule(final Runnable childStatement) {
			final Callable<Object> objectCallable = new Callable<Object>() {
				public Object call() throws Exception {
					childStatement.run();
					return null;
				}
			};
			futures.add(fService.submit(objectCallable));
		}

		public void finished() {
			waitForCompletion();
		}

		public void waitForCompletion() {
			for (Future<Object> each : futures) {
				try {
					each.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
