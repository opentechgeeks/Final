package com.qa.selenium.junit.runners;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.google.common.reflect.ClassPath;

/**
 * Custom version of {@link org.junit.experimental.categories.Categories} that
 * does not throw an exception in
 * {@link #assertNoDescendantsHaveCategoryAnnotations} when using method-level
 * Category annotations with {@link org.junit.runners.Parameterized} test
 * classes. This class also provides the {@link SuiteClasspathClasses}
 * annotation to allow searching all test classes on the classpath for
 * categories.
 * <p>
 * From a given set of test classes, runs only the classes and methods that are
 * annotated with either the category given with the @IncludeCategory
 * annotation, or a subtype of that category.
 * <p>
 * Note that, for now, annotating suites with {@code @Category} has no effect.
 * Categories must be annotated on the direct method or class.
 * <p>
 * Example:
 * 
 * <pre>
 * public interface FastTests {
 * }
 * 
 * public interface SlowTests {
 * }
 * 
 * public interface SmokeTests
 * }
 * 
 * public static class A {
 *     &#064;Test
 *     public void a() {
 *         fail();
 *     }
 * 
 *     &#064;Category(SlowTests.class)
 *     &#064;Test
 *     public void b() {
 *     }
 * 
 *     &#064;Category({FastTests.class, SmokeTests.class})
 *     &#064;Test
 *     public void c() {
 *     }
 * }
 * 
 * &#064;Category({SlowTests.class, FastTests.class})
 * public static class B {
 *     &#064;Test
 *     public void d() {
 *     }
 * }
 * 
 * &#064;RunWith(SuiteCategories.class)
 * &#064;IncludeCategory(SlowTests.class)
 * &#064;SuiteClasses({A.class, B.class})
 * // Note that Categories is a kind of Suite
 * public static class SlowTestSuite {
 *     // Will run A.b and B.d, but not A.a and A.c
 * }
 * </pre>
 * 
 * @version 4.11
 * @author JUnit team
 * @author Matthew DeTullio
 * @see <a href="https://github.com/junit-team/junit/wiki/Categories">Categories
 *      at JUnit wiki</a>
 */
public class SuiteCategories extends Suite {

	private static final String DOT_CLASS = ".class";

	/**
	 * Searches the classpath for test classes inside the given package and its
	 * sub-packages.
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * &#064;RunWith(SuiteCategories.class)
	 * &#064;IncludeCategory(SlowTests.class)
	 * &#064;SuiteClasspathClasses(&quot;com.my.package&quot;)
	 * public static class SlowTestSuite {
	 * 	// Will run any test classes or methods in com.my.package marked with
	 * 	// @Category(SlowTests.class)
	 * }
	 * </pre>
	 * 
	 * @author Matthew DeTullio
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SuiteClasspathClasses {
		/**
		 * Name of the package to search on the classpath for test classes, to
		 * be used in place of {@link SuiteClasses}.
		 */
		public String value();
	}

	/**
	 * From JUnit 4.12-SNAPSHOT
	 */
	static class CategoryFilter extends Filter {
		private final Set<Class<?>> fIncluded;
		private final Set<Class<?>> fExcluded;
		private final boolean fIncludedAny;
		private final boolean fExcludedAny;

		public static CategoryFilter include(boolean matchAny,
				Class<?>... categories) {
			if (hasNull(categories)) {
				throw new NullPointerException("has null category");
			}
			return categoryFilter(matchAny, createSet(categories), true, null);
		}

		public static CategoryFilter include(Class<?> category) {
			return include(true, category);
		}

		public static CategoryFilter include(Class<?>... categories) {
			return include(true, categories);
		}

		public static CategoryFilter exclude(boolean matchAny,
				Class<?>... categories) {
			if (hasNull(categories)) {
				throw new NullPointerException("has null category");
			}
			return categoryFilter(true, null, matchAny, createSet(categories));
		}

		public static CategoryFilter exclude(Class<?> category) {
			return exclude(true, category);
		}

		public static CategoryFilter exclude(Class<?>... categories) {
			return exclude(true, categories);
		}

		public static CategoryFilter categoryFilter(boolean matchAnyInclusions,
				Set<Class<?>> inclusions, boolean matchAnyExclusions,
				Set<Class<?>> exclusions) {
			return new CategoryFilter(matchAnyInclusions, inclusions,
					matchAnyExclusions, exclusions);
		}

		protected CategoryFilter(boolean matchAnyIncludes,
				Set<Class<?>> includes, boolean matchAnyExcludes,
				Set<Class<?>> excludes) {
			fIncludedAny = matchAnyIncludes;
			fExcludedAny = matchAnyExcludes;
			fIncluded = copyAndRefine(includes);
			fExcluded = copyAndRefine(excludes);
		}

		/**
		 * @see #toString()
		 */
		public String describe() {
			return toString();
		}

		/**
		 * Returns string in the form
		 * <tt>&quot;[included categories] - [excluded categories]&quot;</tt>,
		 * where both sets have comma separated names of categories.
		 * 
		 * @return string representation for the relative complement of excluded
		 *         categories set in the set of included categories. Examples:
		 *         <ul>
		 *         <li> <tt>&quot;categories [all]&quot;</tt> for all included
		 *         categories and no excluded ones;
		 *         <li> <tt>&quot;categories [all] - [A, B]&quot;</tt> for all
		 *         included categories and given excluded ones;
		 *         <li> <tt>&quot;categories [A, B] - [C, D]&quot;</tt> for given
		 *         included categories and given excluded ones.
		 *         </ul>
		 * @see Class#toString() name of category
		 */
		public String toString() {
			StringBuilder description = new StringBuilder("categories ")
					.append(fIncluded.isEmpty() ? "[all]" : fIncluded);
			if (!fExcluded.isEmpty()) {
				description.append(" - ").append(fExcluded);
			}
			return description.toString();
		}

		public boolean shouldRun(Description description) {
			if (hasCorrectCategoryAnnotation(description)) {
				return true;
			}

			for (Description each : description.getChildren()) {
				if (shouldRun(each)) {
					return true;
				}
			}

			return false;
		}

		private boolean hasCorrectCategoryAnnotation(Description description) {
			final Set<Class<?>> childCategories = categories(description);

			// If a child has no categories, immediately return.
			if (childCategories.isEmpty()) {
				return fIncluded.isEmpty();
			}

			if (!fExcluded.isEmpty()) {
				if (fExcludedAny) {
					if (matchesAnyParentCategories(childCategories, fExcluded)) {
						return false;
					}
				} else {
					if (matchesAllParentCategories(childCategories, fExcluded)) {
						return false;
					}
				}
			}

			if (fIncluded.isEmpty()) {
				// Couldn't be excluded, and with no suite's included categories
				// treated as should run.
				return true;
			} else {
				if (fIncludedAny) {
					return matchesAnyParentCategories(childCategories,
							fIncluded);
				} else {
					return matchesAllParentCategories(childCategories,
							fIncluded);
				}
			}
		}

		/**
		 * @return <tt>true</tt> if at least one (any) parent category match a
		 *         child, otherwise <tt>false</tt>. If empty
		 *         <tt>parentCategories</tt>, returns <tt>false</tt>.
		 */
		private boolean matchesAnyParentCategories(
				Set<Class<?>> childCategories, Set<Class<?>> parentCategories) {
			for (Class<?> parentCategory : parentCategories) {
				if (hasAssignableTo(childCategories, parentCategory)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * @return <tt>false</tt> if at least one parent category does not match
		 *         children, otherwise <tt>true</tt>. If empty
		 *         <tt>parentCategories</tt>, returns <tt>true</tt>.
		 */
		private boolean matchesAllParentCategories(
				Set<Class<?>> childCategories, Set<Class<?>> parentCategories) {
			for (Class<?> parentCategory : parentCategories) {
				if (!hasAssignableTo(childCategories, parentCategory)) {
					return false;
				}
			}
			return true;
		}

		private static Set<Class<?>> categories(Description description) {
			Set<Class<?>> categories = new HashSet<Class<?>>();
			Collections.addAll(categories, directCategories(description));
			Collections.addAll(categories,
					directCategories(parentDescription(description)));
			return categories;
		}

		private static Description parentDescription(Description description) {
			Class<?> testClass = description.getTestClass();
			return testClass == null ? null : Description
					.createSuiteDescription(testClass);
		}

		private static Class<?>[] directCategories(Description description) {
			if (description == null) {
				return new Class<?>[0];
			}

			Category annotation = description.getAnnotation(Category.class);
			return annotation == null ? new Class<?>[0] : annotation.value();
		}

		private static Set<Class<?>> copyAndRefine(Set<Class<?>> classes) {
			HashSet<Class<?>> c = new HashSet<Class<?>>();
			if (classes != null) {
				c.addAll(classes);
			}
			c.remove(null);
			return c;
		}

		private static boolean hasNull(Class<?>... classes) {
			if (classes == null)
				return false;
			for (Class<?> clazz : classes) {
				if (clazz == null) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * 
	 * @param klass
	 *            the test class annotationed by <code>this</code>
	 * @param builder
	 *            the instance of <code>RunnerBuilder</code> used to build this
	 *            runner
	 * @throws InitializationError
	 *             if neither annotation is present
	 * @throws IOException
	 *             if unable to retrieve classpath
	 */
	public SuiteCategories(Class<?> klass, RunnerBuilder builder)
			throws InitializationError, IOException {
		super(builder, klass, getAnnotatedClasses(klass));
		try {
			Set<Class<?>> included = getIncludedCategory(klass);
			Set<Class<?>> excluded = getExcludedCategory(klass);

			filter(CategoryFilter
					.categoryFilter(true, included, true, excluded));
		} catch (NoTestsRemainException e) {
			throw new InitializationError(e);
		}
		assertNoCategorizedDescendentsOfUncategorizeableParents(getDescription());
	}

	private static Set<Class<?>> getIncludedCategory(Class<?> klass) {
		IncludeCategory annotation = klass.getAnnotation(IncludeCategory.class);
		return createSet(annotation == null ? null : annotation.value());
	}

	private static Set<Class<?>> getExcludedCategory(Class<?> klass) {
		ExcludeCategory annotation = klass.getAnnotation(ExcludeCategory.class);
		return createSet(annotation == null ? null : annotation.value());
	}

	private static void assertNoCategorizedDescendentsOfUncategorizeableParents(
			Description description) throws InitializationError {
		if (!canHaveCategorizedChildren(description)) {
			assertNoDescendantsHaveCategoryAnnotations(description);
		}
		for (Description each : description.getChildren()) {
			assertNoCategorizedDescendentsOfUncategorizeableParents(each);
		}
	}

	private static void assertNoDescendantsHaveCategoryAnnotations(
			Description description) throws InitializationError {
		for (Description each : description.getChildren()) {
			if (each.getAnnotation(Category.class) != null) {
				/*
				 * throw new InitializationError(
				 * "Category annotations on Parameterized classes are not supported on individual methods."
				 * );
				 */
			}
			assertNoDescendantsHaveCategoryAnnotations(each);
		}
	}

	// If children have names like [0], our current magical category code can't
	// determine their parentage.
	private static boolean canHaveCategorizedChildren(Description description) {
		for (Description each : description.getChildren()) {
			if (each.getTestClass() == null) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasAssignableTo(Set<Class<?>> assigns, Class<?> to) {
		for (final Class<?> from : assigns) {
			if (to.isAssignableFrom(from)) {
				return true;
			}
		}
		return false;
	}

	private static Set<Class<?>> createSet(Class<?>... t) {
		final Set<Class<?>> set = new HashSet<Class<?>>();
		if (t != null) {
			Collections.addAll(set, t);
		}
		return set;
	}

	/**
	 * Checks the {@link SuiteClasspathClasses} and {@link SuiteClasses}
	 * annotation for classes.
	 * 
	 * @param klass
	 *            the test class annotationed by <code>this</code>
	 * @return array of classes
	 * @throws InitializationError
	 *             if neither annotation is present
	 * @throws IOException
	 *             if unable to retrieve classpath
	 */
	private static Class<?>[] getAnnotatedClasses(Class<?> klass)
			throws InitializationError, IOException {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		SuiteClasses scAnnotation = klass.getAnnotation(SuiteClasses.class);

		SuiteClasspathClasses sccAnnotation = klass
				.getAnnotation(SuiteClasspathClasses.class);

		if (scAnnotation == null && sccAnnotation == null) {
			throw new InitializationError(
					String.format(
							"class '%s' must have a SuiteClasses or SuiteClasspathClasses annotation",
							new Object[] { klass.getName() }));
		}

		// Retrieve named classes
		if (scAnnotation != null) {
			classes.addAll(Arrays.asList(scAnnotation.value()));
		}

		// Retrieve classes in package
		if (sccAnnotation != null) {
			classes.addAll(getClassesInPackage(sccAnnotation.value()));
		}

		/*
		 * Remove classes with SuiteClasses or SuiteClasspathClasses annotation
		 * because this can unintentionally add test classes with varying
		 * parents
		 */
		Iterator<Class<?>> iter = classes.iterator();
		while (iter.hasNext()) {
			Class<?> c = iter.next();
			if (c.getAnnotation(SuiteClasses.class) != null
					|| c.getAnnotation(SuiteClasspathClasses.class) != null) {
				iter.remove();
			}
		}

		return classes.toArray(new Class<?>[classes.size()]);

	}

	/**
	 * Uses Guava to get all classes in a given package and its sub-packages.
	 * 
	 * @param packageName
	 *            in format <code>com/my/package</code> or
	 *            <code>com.my.package</code>
	 * @return array of classes
	 * @throws IOException
	 *             if unable to retrieve classpath
	 */
	private static Set<Class<?>> getClassesInPackage(String packageName)
			throws IOException {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		for (ClassPath.ClassInfo classInfo : ClassPath.from(
				Thread.currentThread().getContextClassLoader())
				.getTopLevelClassesRecursive(packageName)) {
			try {
				String className = classInfo.getResourceName();

				if (className.endsWith(DOT_CLASS)) {
					classes.add(Class.forName(className.substring(0,
							className.length() - DOT_CLASS.length()).replace(
							'/', '.')));
				}
			} catch (ClassNotFoundException e) {
				// Eat the exception, try the next class
			}
		}

		return classes;
	}
}
