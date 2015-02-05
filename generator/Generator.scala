/*     / \____  _    ______   _____ / \____   ____  _____
 *    /  \__  \/ \  / \__  \ /  __//  \__  \ /    \/ __  \   Javaslang
 *  _/  // _\  \  \/  / _\  \\_  \/  // _\  \  /\  \__/  /   Copyright 2014-2015 Daniel Dietrich
 * /___/ \_____/\____/\_____/____/\___\_____/_/  \_/____/    Licensed under the Apache License, Version 2.0
 */

import java.io.File
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.StandardOpenOption

import GeneratorImplicits._

import scala.collection.mutable
import scala.util.Properties.lineSeparator

val N = 26
val TARGET = "src-gen/main/java"

/**
 * ENTRY POINT
 */
def run() {
  genFunctions()
  genFunctors()
  genHigherKindeds()
  genMonads()
  genPropertyChecks()
  genTuples()
}

// Use /$** instead of /** in a StringContext when IntelliJ IDEA otherwise shows up errors in the editor
val javadoc = "**"

/**
 * Generator of javaslang.algebra.Functor*
 */
def genFunctors() = 1 to N foreach { i =>
  genJavaslangFile("javaslang.algebra", s"Functor$i")((im: ImportManager, packageName, className) => {
    val generics = (1 to i).gen(j => s"T$j")(", ")
    val paramTypes = (1 to i).gen(j => s"? super T$j")(", ")
    val resultType = if (i == 1) "? extends U1" else s"${im.getType(s"javaslang.Tuple$i")}<${(1 to i).gen(j => s"? extends U$j")(", ")}>"
    val resultGenerics = (1 to i).gen(j => s"U$j")(", ")
    val functionType = i match {
      case 1 => im.getType("java.util.function.Function")
      case 2 => im.getType("java.util.function.BiFunction")
      case _ => im.getType(s"javaslang.Function$i")
    }
    xs"""
      ${(i == 1).gen(xs"""
      /$javadoc
       * Defines a Functor by generalizing the map function.
       * <p>
       * All instances of the Functor interface should obey the two functor laws:
       * <ul>
       *     <li>{@code m.map(a -> a) ≡ m}</li>
       *     <li>{@code m.map(f.compose(g)) ≡ m.map(g).map(f)}</li>
       * </ul>
       *
       * @param <T1> Component type of this Functor.
       * @see <a href="http://www.haskellforall.com/2012/09/the-functor-design-pattern.html">The functor design pattern</a>
       */
      """)}
      public interface $className<$generics> {

          <$resultGenerics> $className<$resultGenerics> map($functionType<$paramTypes, $resultType> f);
      }
  """
  })
}

/**
 * Generator of javaslang.algebra.HigherKinded*
 */
def genHigherKindeds() = 1 to N foreach { i =>
  genJavaslangFile("javaslang.algebra", s"HigherKinded$i")((im: ImportManager, packageName, className) => xs"""
      ${(i == 1).gen(xs"""
      /$javadoc
       * <p>
       * A type <em>HigherKinded</em> declares a generic type constructor, which consists of an inner type (component type)
       * and an outer type (container type).
       * </p>
       * <p>
       * HigherKinded is needed to (partially) simulate Higher-Kinded/Higher-Order Types, which  are not part of the Java
       * language but needed for generic type constructors.
       * </p>
       * <p>
       * Example: {@link javaslang.algebra.Monad#flatMap(java.util.function.Function)}
       * </p>
       *
       * @param <T1> Component type of the type to be constructed.
       * @param <TYPE> Container type of the type to be constructed.
       */
      """)}
      public interface $className<${(1 to i).gen(j => s"T$j")(", ")}, TYPE extends $className<${"?, " * i}TYPE>> {

          // used for type declaration only
      }
  """)
}

/**
 * Generator of javaslang.algebra.Monad*
 */
def genMonads() = 1 to N foreach { i =>
  genJavaslangFile("javaslang.algebra", s"Monad$i")((im: ImportManager, packageName, className) => {
    val generics = (1 to i).gen(j => s"T$j")(", ")
    val paramTypes = (1 to i).gen(j => s"? super T$j")(", ")
    val resultGenerics = (1 to i).gen(j => s"U$j")(", ")
    val functionType = i match {
      case 1 => im.getType("java.util.function.Function")
      case 2 => im.getType("java.util.function.BiFunction")
      case _ => im.getType(s"javaslang.Function$i")
    }
    xs"""
      ${(i == 1).gen(xs"""
      /$javadoc
       * Defines a Monad by generalizing the flatMap and unit functions.
       * <p>
       * All instances of the Monad interface should obey the three control laws:
       * <ul>
       *     <li><strong>Left identity:</strong> {@code unit(a).flatMap(f) ≡ f a}</li>
       *     <li><strong>Right identity:</strong> {@code m.flatMap(unit) ≡ m}</li>
       *     <li><strong>Associativity:</strong> {@code m.flatMap(f).flatMap(g) ≡ m.flatMap(x -> f.apply(x).flatMap(g)}</li>
       * </ul>
       * <p>
       *
       * @param <T1> Component type of this Monad$i.
       */
      """)}
      public interface $className<$generics, M extends HigherKinded$i<${"?, " * i}M>> extends Functor$i<$generics>, HigherKinded$i<$generics, M> {

          <$resultGenerics, MONAD extends HigherKinded$i<$resultGenerics, M>> $className<$resultGenerics, M> flatMap($functionType<$paramTypes, MONAD> f);
      }
  """
  })
}

/**
 * Generator of javaslang.test.Property
 */
def genPropertyChecks(): Unit = {

  def genProperty(im: ImportManager, packageName: String, className: String): String = xs"""
    @FunctionalInterface
    public interface $className {

        /**
         * A thread-safe, equally distributed random number generator.
         */
        ${im.getType("java.util.function.Supplier")}<${im.getType("java.util.Random")}> RNG = ${im.getType("java.util.concurrent.ThreadLocalRandom")}::current;

        /**
         * Default size hint for generators.
         */
        int DEFAULT_SIZE = 100;

        /**
         * Default tries to check a property.
         */
        int DEFAULT_TRIES = 1000;

        CheckResult check(${im.getType("java.util.Random")} randomNumberGenerator, int size, int tries);

        default CheckResult check(int size, int tries) {
            return check(RNG.get(), size, tries);
        }

        default CheckResult check() {
            return check(RNG.get(), DEFAULT_SIZE, DEFAULT_TRIES);
        }

        default Property and(Property property) {
            return (rng, size, tries) -> {
                final CheckResult result = check(rng, size, tries);
                if (result.isSatisfied()) {
                    return property.check(rng, size, tries);
                } else {
                    return result;
                }
            };
        }

        default Property or(Property property) {
            return (rng, size, tries) -> {
                final CheckResult result = check(rng, size, tries);
                if (result.isSatisfied()) {
                    return result;
                } else {
                    return property.check(rng, size, tries);
                }
            };
        }

        ${(1 to N).gen(i => {
            val generics = (1 to i).gen(j => s"T$j")(", ")
            val parameters = (1 to i).gen(j => s"a$j")(", ")
            val parametersDecl = (1 to i).gen(j => s"Arbitrary<T$j> a$j")(", ")
            xs"""
                static <$generics> ForAll$i<$generics> forAll($parametersDecl) {
                    return new ForAll$i<>($parameters);
                }
            """
        })("\n\n")}

        ${(1 to N).gen(i => {
            val generics = (1 to i).gen(j => s"T$j")(", ")
            val params = (name: String) => (1 to i).gen(j => s"$name$j")(", ")
            val parametersDecl = (1 to i).gen(j => s"Arbitrary<T$j> a$j")(", ")
            xs"""
                static class ForAll$i<$generics> {

                    ${(1 to i).gen(j => xs"""
                        private final Arbitrary<T$j> a$j;
                    """)("\n")}

                    ForAll$i($parametersDecl) {
                        ${(1 to i).gen(j => xs"""
                            this.a$j = a$j;
                        """)("\n")}
                    }

                    public Property$i<$generics> suchThat(${im.getType(s"javaslang.CheckedFunction$i")}<$generics, Boolean> predicate) {
                        final ${im.getType(s"javaslang.CheckedFunction$i")}<$generics, Condition> proposition = (${params("t")}) -> new Condition(true, predicate.apply(${params("t")}));
                        return new Property$i<>(${params("a")}, proposition);
                    }
                }
            """
        })("\n\n")}

        ${(1 to N).gen(i => {

            val checkedFunctionType = im.getType(s"javaslang.CheckedFunction$i")
            val failureType = im.getType("javaslang.control.Failure")
            val noneType = im.getType("javaslang.control.None")
            val randomType = im.getType("java.util.Random")
            val someType = im.getType("javaslang.control.Some")
            val tryType = im.getType("javaslang.control.Try")
            val tupleType = im.getType(s"javaslang.Tuple")
            val tuple_iType = im.getType(s"javaslang.Tuple$i")

            val generics = (1 to i).gen(j => s"T$j")(", ")
            val params = (paramName: String) => (1 to i).gen(j => s"$paramName$j")(", ")
            val parametersDecl = (1 to i).gen(j => s"Arbitrary<T$j> a$j")(", ")

            xs"""
                static class Property$i<$generics> implements Property {

                    ${(1 to i).gen(j => xs"""
                        private final Arbitrary<T$j> a$j;
                    """)("\n")}
                    final $checkedFunctionType<$generics, Condition> predicate;

                    Property$i($parametersDecl, $checkedFunctionType<$generics, Condition> predicate) {
                        ${(1 to i).gen(j => xs"""
                            this.a$j = a$j;
                        """)("\n")}
                        this.predicate = predicate;
                    }

                    public Property implies($checkedFunctionType<$generics, Boolean> postcondition) {
                        final $checkedFunctionType<$generics, Condition> implication = (${params("t")}) -> {
                            final Condition precondition = predicate.apply(${params("t")});
                            if (precondition.isFalse()) {
                                // ex falso quodlibet
                                return new Condition(false, true);
                            } else {
                                return new Condition(true, postcondition.apply(${params("t")}));
                            }
                        };
                        return new Property$i<>(${params("a")}, implication);
                    }

                    @Override
                    public CheckResult<$tuple_iType<$generics>> check($randomType random, int size, int tries) {
                        try {
                            ${(1 to i).gen(j => {
                                s"""final Gen<T$j> gen$j = $tryType.of(() -> a$j.apply(size)).recover(x -> { throw Errors.arbitraryError($j, size, x); }).get();"""
                            })("\n")}
                            boolean exhausted = true;
                            for (int i = 1; i <= tries; i++) {
                                try {
                                    ${(1 to i).gen(j => {
                                      s"""final T$j val$j = $tryType.of(() -> gen$j.apply(random)).recover(x -> { throw Errors.genError($j, size, x); }).get();"""
                                    })("\n")}
                                    try {
                                        final Condition condition = $tryType.of(() -> predicate.apply(${(1 to i).gen(j => s"val$j")(", ")})).recover(x -> { throw Errors.predicateError(x); }).get();
                                        if (condition.precondition) {
                                            exhausted = false;
                                            if (!condition.postcondition) {
                                                return CheckResult.falsified(i, $tupleType.of(${(1 to i).gen(j => s"val$j")(", ")}));
                                            }
                                        }
                                    } catch($failureType.NonFatal nonFatal) {
                                        return CheckResult.erroneous(i, (Error) nonFatal.getCause(), new $someType<>($tupleType.of(${(1 to i).gen(j => s"val$j")(", ")})));
                                    }
                                } catch($failureType.NonFatal nonFatal) {
                                    return CheckResult.erroneous(i, (Error) nonFatal.getCause(), $noneType.instance());
                                }
                            }
                            return CheckResult.satisfied(tries, exhausted);
                        } catch($failureType.NonFatal nonFatal) {
                            return CheckResult.erroneous(0, (Error) nonFatal.getCause(), $noneType.instance());
                        }
                    }
                }
            """
        })("\n\n")}

        static class Condition {

            final boolean precondition;
            final boolean postcondition;

            Condition(boolean precondition, boolean postcondition) {
                this.precondition = precondition;
                this.postcondition = postcondition;
            }

            // ¬(p => q) ≡ ¬(¬p ∨ q) ≡ p ∧ ¬q
            boolean isFalse() {
                return precondition && !postcondition;
            }
        }
    }
  """

  genJavaslangFile("javaslang.test", "Property")(genProperty)
}

/**
 * Generator of Functions
 */
def genFunctions(): Unit = {

  def genFunctions(i: Int): Unit = {

    val generics = (1 to i).gen(j => s"T$j")(", ")
    val genericsReversed = (1 to i).reverse.gen(j => s"T$j")(", ")
    val genericsTuple = if (i > 0) s"<$generics>" else ""
    val genericsFunction = if (i > 0) s"$generics, " else ""
    val genericsReversedFunction = if (i > 0) s"$genericsReversed, " else ""
    val curried = if (i == 0) "v" else (1 to i).gen(j => s"t$j")(" -> ")
    val paramsDecl = (1 to i).gen(j => s"T$j t$j")(", ")
    val params = (1 to i).gen(j => s"t$j")(", ")
    val paramsReversed = (1 to i).reverse.gen(j => s"t$j")(", ")
    val tupled = (1 to i).gen(j => s"t._$j")(", ")

    def additionalInterfaces(arity: Int, checked: Boolean): String = (arity, checked) match {
      case (0, false) => s", java.util.function.Supplier<R>"
      case (1, false) => s", java.util.function.Function<$generics, R>"
      case (2, false) => s", java.util.function.BiFunction<$generics, R>"
      case _ => ""
    }

    def returnType(max: Int, function: String): String = {
      if (max == 0) {
          s"${function}1<Void, R>"
      } else {
          def returnType(curr: Int, max: Int): String = {
              val isParam = curr < max
              val next = if (isParam) returnType(curr + 1, max) else "R"
              s"${function}1<T$curr, $next>"
          }
          returnType(1, max)
      }
    }

    def genFunction(name: String, checked: Boolean)(im: ImportManager, packageName: String, className: String): String = xs"""
      @FunctionalInterface
      public interface $className<${if (i > 0) s"$generics, " else ""}R> extends λ<R>${additionalInterfaces(i, checked)} {

          ${if (i == 1) xs"""
          static <T> ${name}1<T, T> identity() {
              return t -> t;
          }""" else ""}

          ${if ((i == 1 || i == 2) && !checked) "@Override" else ""}
          R apply($paramsDecl)${if (checked) " throws Throwable" else ""};

          ${if (i == 0 && !checked) xs"""
          @Override
          default R get() {
              return apply();
          }""" else ""}

          @Override
          default int arity() {
              return $i;
          }

          @Override
          default ${returnType(i, name)} curried() {
              ${(i == 1).gen("//noinspection Convert2MethodRef")}
              return $curried -> apply($params);
          }

          @Override
          default ${name}1<Tuple$i$genericsTuple, R> tupled() {
              return t -> apply($tupled);
          }

          @Override
          default $className<${genericsReversedFunction}R> reversed() {
              ${(i <= 1).gen("//noinspection Convert2MethodRef")}
              return ($paramsReversed) -> apply($params);
          }

          @Override
          default <V> $className<${genericsFunction}V> andThen(${im.getType("java.util.function.Function")}<? super R, ? extends V> after) {
              ${im.getType("java.util.Objects")}.requireNonNull(after);
              return ($params) -> after.apply(apply($params));
          }

          ${if (i == 1) xs"""
          default <V> ${name}1<V, R> compose(${im.getType("java.util.function.Function")}<? super V, ? extends T1> before) {
              ${im.getType("java.util.Objects")}.requireNonNull(before);
              return v -> apply(before.apply(v));
          }""" else ""}
      }
    """

    genJavaslangFile("javaslang", s"CheckedFunction$i")(genFunction("CheckedFunction", checked = true))
    genJavaslangFile("javaslang", s"Function$i")(genFunction("Function", checked = false))
  }

  (0 to N).foreach(genFunctions)
}

/**
 * Generator of javaslang.Tuple*
 */
def genTuples(): Unit = {

  /*
   * Generates Tuple0
   */
  def genTuple0(im: ImportManager, packageName: String, className: String): String = xs"""
    /**
     * Implementation of an empty tuple, a tuple containing no elements.
     */
    public final class $className implements Tuple {

        private static final long serialVersionUID = 1L;

        /**
         * The singleton instance of $className.
         */
        private static final $className INSTANCE = new $className();

        /**
         * Hidden constructor.
         */
        private $className() {
        }

        /**
         * Returns the singleton instance of $className.
         *
         * @return The singleton instance of $className.
         */
        public static $className instance() {
            return INSTANCE;
        }

        @Override
        public int arity() {
            return 0;
        }

        @Override
        public $className unapply() {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return ${im.getType("java.util.Objects")}.hash();
        }

        @Override
        public String toString() {
            return "()";
        }

        // -- Serializable implementation

        /**
         * Instance control for object serialization.
         *
         * @return The singleton instance of $className.
         * @see java.io.Serializable
         */
        private Object readResolve() {
            return INSTANCE;
        }
    }
  """

  /*
   * Generates Tuple1..N
   */
  def genTuple(i: Int)(im: ImportManager, packageName: String, className: String): String = {
    val generics = (1 to i).gen(j => s"T$j")(", ")
    val paramsDecl = (1 to i).gen(j => s"T$j t$j")(", ")
    val params = (1 to i).gen(j => s"_$j")(", ")
    val paramTypes = (1 to i).gen(j => s"? super T$j")(", ")
    val resultType = if (i == 1) "? extends U1" else s"Tuple$i<${(1 to i).gen(j => s"? extends U$j")(", ")}>"
    val resultGenerics = (1 to i).gen(j => s"U$j")(", ")
    val untyped = (1 to i).gen(j => "?")(", ")
    val functionType = i match {
      case 1 => im.getType("java.util.function.Function")
      case 2 => im.getType("java.util.function.BiFunction")
      case _ => s"Function$i"
    }

    xs"""
      /**
       * Implementation of a pair, a tuple containing $i elements.
       */
      public class $className<$generics> implements Tuple, ${im.getType(s"javaslang.algebra.Monad$i")}<$generics, $className<$untyped>> {

          private static final long serialVersionUID = 1L;

          ${(1 to i).gen(j => s"public final T$j _$j;")("\n")}

          public $className($paramsDecl) {
              ${(1 to i).gen(j => s"this._$j = t$j;")("\n")}
          }

          @Override
          public int arity() {
              return $i;
          }

          @SuppressWarnings("unchecked")
          @Override
          public <$resultGenerics, MONAD extends ${im.getType(s"javaslang.algebra.HigherKinded$i")}<$resultGenerics, $className<$untyped>>> $className<$resultGenerics> flatMap($functionType<$paramTypes, MONAD> f) {
              return ($className<$resultGenerics>) f.apply($params);
          }

          ${(i > 1).gen("""@SuppressWarnings("unchecked")""")}
          @Override
          public <$resultGenerics> $className<$resultGenerics> map($functionType<$paramTypes, $resultType> f) {
              ${if (i > 1) { xs"""
                // normally the result of f would be mapped to the result type of map, but Tuple.map is a special case
                return ($className<$resultGenerics>) f.apply($params);"""
              } else { xs"""
                return new $className<>(f.apply($params));"""
              }}
          }

          @Override
          public $className<$generics> unapply() {
              return this;
          }

          @Override
          public boolean equals(Object o) {
              if (o == this) {
                  return true;
              } else if (!(o instanceof $className)) {
                  return false;
              } else {
                  final $className that = ($className) o;
                  return ${(1 to i).gen(j => s"${im.getType("java.util.Objects")}.equals(this._$j, that._$j)")("\n                         && ")};
              }
          }

          @Override
          public int hashCode() {
              return ${im.getType("java.util.Objects")}.hash(${(1 to i).gen(j => s"_$j")(", ")});
          }

          @Override
          public String toString() {
              return String.format("(${(1 to i).gen(_ => s"%s")(", ")})", ${(1 to i).gen(j => s"_$j")(", ")});
          }
      }
    """
  }

  /*
   * Generates Tuple
   */
  def genBaseTuple(im: ImportManager, packageName: String, className: String): String = {

    def genFactoryMethod(i: Int) = {
      val generics = (1 to i).gen(j => s"T$j")(", ")
      val paramsDecl = (1 to i).gen(j => s"T$j t$j")(", ")
      val params = (1 to i).gen(j => s"t$j")(", ")
      xs"""
        static <$generics> Tuple$i<$generics> of($paramsDecl) {
            return new Tuple$i<>($params);
        }
      """
    }

    xs"""
      public interface $className extends ValueObject {

          /**
           * Returns the number of elements of this tuple.
           *
           * @return The number of elements.
           */
          int arity();

          // -- factory methods

          static Tuple0 empty() {
              return Tuple0.instance();
          }

          ${(1 to N).gen(genFactoryMethod)("\n\n")}
      }
    """
  }

  genJavaslangFile("javaslang", "Tuple")(genBaseTuple)
  genJavaslangFile("javaslang", "Tuple0")(genTuple0)

  (1 to N).foreach { i =>
    genJavaslangFile("javaslang", s"Tuple$i")(genTuple(i))
  }
}

/**
 * Adds the Javaslang header to generated classes.
 * @param packageName Java package name
 * @param className Simple java class name
 * @param gen A generator which produces a String.
 */
def genJavaslangFile(packageName: String, className: String)(gen: (ImportManager, String, String) => String, knownSimpleClassNames: List[String] = List()) =
  genJavaFile(packageName, className)(xraw"""
    /**    / \____  _    ______   _____ / \____   ____  _____
     *    /  \__  \/ \  / \__  \ /  __//  \__  \ /    \/ __  \   Javaslang
     *  _/  // _\  \  \/  / _\  \\_  \/  // _\  \  /\  \__/  /   Copyright 2014-2015 Daniel Dietrich
     * /___/ \_____/\____/\_____/____/\___\_____/_/  \_/____/    Licensed under the Apache License, Version 2.0
     */
  """)(gen)//(StandardCharsets.UTF_8)

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
     J A V A   G E N E R A T O R   F R A M E W O R K
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

/**
 * Generates a Java file.
 * @param packageName Java package name
 * @param className Simple java class name
 * @param classHeader A class file header
 * @param gen A generator which produces a String.
 */
def genJavaFile(packageName: String, className: String)(classHeader: String)(gen: (ImportManager, String, String) => String, knownSimpleClassNames: List[String] = List())(implicit charset: Charset = StandardCharsets.UTF_8): Unit = {

  val dirName = packageName.replaceAll("\\.", File.separator)
  val fileName = className + ".java"
  val importManager = new ImportManager(packageName, knownSimpleClassNames)
  val classBody = gen.apply(importManager, packageName, className)

  genFile(dirName, fileName)(xraw"""
    $classHeader
    package $packageName;

    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
       G E N E R A T O R   C R A F T E D
    \*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

    ${importManager.getImports}

    $classBody
  """)
}

/**
 * An ImportManager which generates an import section of a Java class file.
 * @param packageNameOfClass package name of the generated class
 * @param knownSimpleClassNames a list of class names which may not be imported from other packages
 */
class ImportManager(packageNameOfClass: String, knownSimpleClassNames: List[String], wildcardThreshold: Int = 5) {

  val nonStaticImports = new mutable.HashMap[String, String]
  val staticImports = new mutable.HashMap[String, String]

  def getType(fullQualifiedName: String): String = simplify(fullQualifiedName, nonStaticImports)

  def getStatic(fullQualifiedName: String): String = simplify(fullQualifiedName, staticImports)

  private def getPackageName(fqn: String): String = fqn.substring(0, Math.max(fqn.lastIndexOf("."), 0))
  private def getSimpleName(fqn: String): String = fqn.substring(fqn.lastIndexOf(".") + 1)

  private def simplify(fullQualifiedName: String, imports: mutable.HashMap[String, String]): String = {
    val simpleName = getSimpleName(fullQualifiedName)
    val packageName = getPackageName(fullQualifiedName)
    if (packageName.isEmpty && !packageNameOfClass.isEmpty) {
      throw new IllegalStateException(s"Can't import class '$simpleName' located in default package")
    } else if (packageName == packageNameOfClass) {
      simpleName
    } else if (imports.contains(fullQualifiedName)) {
      imports.get(fullQualifiedName).get
    } else if (knownSimpleClassNames.contains(simpleName) || imports.values.exists(simpleName.equals(_))) {
      fullQualifiedName
    } else {
      imports += fullQualifiedName -> simpleName
      simpleName
    }
  }

  def getImports: String = {

    def optimizeImports(imports: Seq[String], static: Boolean): String = {
      val counts = imports.map(getPackageName).groupBy(s => s).map { case (s, list) => s -> list.length }
      val directImports = imports.filter(s => counts(getPackageName(s)) <= wildcardThreshold)
      val wildcardImports = counts.filter { case (_, count) => count > wildcardThreshold }.keySet.toIndexedSeq.map(s => s"$s.*")
      (directImports ++ wildcardImports).sorted.map(fqn => s"import ${static.gen("static ")}$fqn;").mkString("\n")
    }

    val staticImportSection = optimizeImports(staticImports.keySet.toIndexedSeq, static = true)
    val nonStaticImportSection = optimizeImports(nonStaticImports.keySet.toIndexedSeq, static = false)
    Seq(staticImportSection, nonStaticImportSection).mkString("\n\n")
  }
}

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
     C O R E   G E N E R A T O R   F R A M E W O R K
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

def genFile(dirName: String, fileName: String)(contents: => String)(implicit charset: Charset = StandardCharsets.UTF_8): Unit = {

  println(s"Generating $dirName${File.separator}$fileName")

  import java.nio.file.{Paths, Files}

  Files.write(
    Files.createDirectories(Paths.get(TARGET, dirName)).resolve(fileName),
    contents.getBytes(charset),
    StandardOpenOption.CREATE, StandardOpenOption.WRITE)
}

/**
 * Core generator API
 */
object GeneratorImplicits {

  implicit class BooleanExtensions(condition: Boolean) {
    def gen(s: String): String =  if (condition) s else ""
  }

  /**
   * Generates a String based on ints within a specific range.
   * {{{
   * (1 to 3).gen(i => s"x$i")(", ") // x1, x2, x3
   * (1 to 3).reverse.gen(i -> s"x$i")(", ") // x3, x2, x1
   * }}}
   * @param range A Range
   */
  implicit class RangeExtensions(range: Range) {
    def gen(f: Int => String = String.valueOf)(implicit delimiter: String = ""): String =
    range map f mkString delimiter
  }

  /**
   * Generates a String based on a sequence of objects. Objects are converted to Strings via toString.
   * {{{
   * // val a = "A"
   * // val b = "B"
   * // val c = "C"
   * Seq("a", "b", "c").gen(s => raw"""val $s = "${s.toUpperCase}"""")("\n")
   * }}}
   * @param seq A Seq
   */
  implicit class SeqExtensions(seq: Seq[Any]) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      seq.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple1Extensions(tuple: Tuple1[Any]) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      f.apply(tuple._1.toString) mkString delimiter
  }

  implicit class Tuple2Extensions(tuple: (Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  /**
   * Generates a String based on a tuple of objects. Objects are converted to Strings via toString.
   * {{{
   * // val seq = Seq("a", "1", "true")
   * s"val seq = Seq(${("a", 1, true).gen(s => s""""$s"""")(", ")})"
   * }}}
   * @param tuple A Tuple
   */
  implicit class Tuple3Extensions(tuple: (Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple4Extensions(tuple: (Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple5Extensions(tuple: (Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple6Extensions(tuple: (Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple7Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple8Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple9Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple10Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple11Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple12Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple13Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple14Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple15Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple16Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple17Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple18Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple19Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple20Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple21Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  implicit class Tuple22Extensions(tuple: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)) {
    def gen(f: String => String = identity)(implicit delimiter: String = ""): String =
      tuple.productIterator.toList.map(x => f.apply(x.toString)) mkString delimiter
  }

  /**
   * Provides StringContext extensions, e.g. indentation of cascaded rich strings.
   * @param sc Current StringContext
   * @see <a href="https://gist.github.com/danieldietrich/5174348">this gist</a>
   */
  implicit class StringContextExtensions(sc: StringContext) {

    /**
     * Formats escaped strings.
     * @param args StringContext parts
     * @return An aligned String
     */
    def xs(args: Any*): String = align(sc.s, args)

    /**
     * Formats raw/unescaped strings.
     * @param args StringContext parts
     * @return An aligned String
     */
    def xraw(args: Any*): String = align(sc.raw, args)

    /**
     * Indenting a rich string, removing first and last newline.
     * A rich string consists of arguments surrounded by text parts.
     */
    private def align(interpolator: Seq[Any] => String, args: Seq[Any]): String = {

      // indent embedded strings, invariant: parts.length = args.length + 1
      val indentedArgs = for {
        (part, arg) <- sc.parts zip args.map(s => if (s == null) "" else s.toString)
      } yield {
        // get the leading space of last line of current part
        val space = """([ \t]*)[^\s]*$""".r.findFirstMatchIn(part).map(_.group(1)).getOrElse("")
        // add this leading space to each line (except the first) of current arg
        arg.split("\r?\n") match {
            case lines: Array[String] if lines.length > 0 => lines reduce (_ + lineSeparator + space + _)
            case whitespace => whitespace mkString ""
        }
      }

      // remove first and last newline and split string into separate lines
      // adding termination symbol \u0000 in order to preserve empty strings between last newlines when splitting
      val split = (interpolator(indentedArgs).replaceAll( """(^[ \t]*\r?\n)|(\r?\n[ \t]*$)""", "") + '\u0000').split("\r?\n")

      // find smallest indentation
      val prefix = split filter (!_.trim().isEmpty) map { s =>
        """^\s+""".r.findFirstIn(s).getOrElse("")
      } match {
        case prefixes: Array[String] if prefixes.length > 0 => prefixes reduce { (s1, s2) =>
          if (s1.length <= s2.length) s1 else s2
        }
        case _ => ""
      }

      // align all lines
      val aligned = split map { s =>
        if (s.startsWith(prefix)) s.substring(prefix.length) else s
      } mkString lineSeparator dropRight 1 // dropping termination character \u0000

      // combine multiple newlines to two
      aligned.replaceAll("""[ \t]*\r?\n ([ \t]*\r?\n)+""", lineSeparator * 2)
    }
  }
}
