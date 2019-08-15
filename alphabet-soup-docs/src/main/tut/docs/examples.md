---
layout: docs
title: Examples
---

# Worked Examples

```scala mdoc:invisible
import io.typechecked.alphabetsoup._
import io.typechecked.alphabetsoup.macros._
import cats.mtl.ApplicativeAsk
import cats.mtl.MonadState
import cats.syntax.functor._
import cats.Functor
import cats.Monad

@Atomic trait A; @Atomic trait B; @Atomic trait C; @Atomic trait D
trait F[T]
implicit val fFunctor: Functor[F] = null
```

First, some value classes we'll be using in all the examples:

```scala mdoc:silent
@Atomic case class Username(value: String)
@Atomic case class UserId(value: Int)
@Atomic case class Gender(value: String)
@Atomic case class FavouriteAnimal(value: String)
```

## Example 1: Boilerplate reduction

You probably have a case class representing a database table, and a different class to return it in the API.

Some times (or most of the time), these are probably very similar or identical. But they need to be separate so you
don't automatically reveal new database information. Or because you don't want to reveal sensitive information such
as the user's internal id.

So imagine the following:

```scala mdoc:silent
case class DbUser(
  id: UserId,
  name: Username,
  gender: Gender,
  favouriteAnimal: FavouriteAnimal
)

case class ApiUser(
  name: Username,
  animal: FavouriteAnimal,
  gender: Gender
)
```

Now, to turn one into the other you would usually have to write:

```scala mdoc:silent
def dbToApi(u: DbUser): ApiUser = ApiUser(
  name = u.name,
  animal = u.favouriteAnimal,
  gender = u.gender
)
```

At no point above did we make a choice: that code could write itself. And, indeed, it does! With a `Mixer`:

```scala mdoc:silent
def dbToApi2(u: DbUser): ApiUser = Mixer[DbUser, ApiUser].mix(u)
```

Database and API classes can get pretty big, and intertwined. Alphabet soup cuts out a _lot_ of repetitive boilerplate.

## Example 2: `ApplicativeAsk`

Suppose you have the following `ApplicativeAsk` instance, for some functor `F[_]`:

```scala mdoc:silent
val aa: ApplicativeAsk[F, (A, B, C, D)] = null
```

and you wanted to read `(B, D)`. You would have to do the following:

```scala mdoc:compile-only
val result: F[(B, D)] = aa.ask.map(tuple => tuple._2 -> tuple._4)
```

It's even worse if the tuple is larger, or nested, or contains case classes to project as well.

You might be in a context where `A` and `C` don't even make sense, so you want to pass in an `ApplicativeAsk[F, (B, D)]`
instead, forgetting about `A` and `C`. This would be several lines of canonical code.

Luckily, alphabet soup can reduce this to nothing at the call-site:

```scala mdoc:silent
import shapeless.=:!=

implicit def getAA[M[_], X, Y](
  implicit ax: ApplicativeAsk[M, X],
  ev: X =:!= Y,
  m: Mixer[X, Y]
): ApplicativeAsk[M, Y] = new ApplicativeAsk[M, Y] {
  val applicative = ax.applicative
  def ask: M[Y] = applicative.map(ax.ask)(m.mix)
  def reader[Z](f: Y => Z): M[Z] = ax.reader[Z](a => f(m.mix(a)))
}
```

Now, we have our original `ApplicativeAsk`:

``` scala mdoc:silent
implicit val applicativeAsk: ApplicativeAsk[F, (A, B, C, D)] = null
```

and we can get any sub-`ApplicativeAsk`, for free:

```scala mdoc:compile-only
implicitly[ApplicativeAsk[F, (B, D)]]
implicitly[ApplicativeAsk[F, (B, A)]]
implicitly[ApplicativeAsk[F, (C, D, B)]]
implicitly[ApplicativeAsk[F, B]]
```

## Example 3: `MonadState`

This is the same problem as `ApplicativeAsk`. You might have the following `MonadState` instance for some monad `F[_]`:

```scala mdoc:silent
val ms: MonadState[F, (A, B, C, D)] = null
```

Suppose you had a function `f: (B, D) => (B, D)`, to mutate the `B` and `D` values in that tuple. What would you have to do?

```scala mdoc:invisible
val f: (B, D) => (B, D) = null
```

```scala mdoc:compile-only
ms.modify { case (a, b, c, d) =>
  val (b2, d2) = f(b, d)
  (a, b2, c, d2)
}
```

Awful! What if it was a case class, or nested, or even larger? As with above that code has no choices in it, it is
canonical. Alphabet soup can write that code for you:

```scala mdoc
implicit def projectMonadState[M[_], X, Y](
  implicit ms: MonadState[M, X],
  ev: X =:!= Y,
  m: Mixer[X, Y]
): MonadState[M, Y] = new MonadState[M, Y] {
  val monad: Monad[M] = ms.monad

  def get: M[Y] = monad.map(ms.get)(m.mix)

  def set(y: Y): M[Unit] = ms.modify(x => m.inject(y, x))

  def inspect[T](f: Y => T): M[T] = ms.inspect(x => f(m.mix(x)))

  def modify(f: Y => Y): M[Unit] = ms.modify(x => m.modify(f)(x))
}
```

Here's a quick rundown of what's happening:

### The arguments

The implicit arguments are exactly the same as the `ApplicativeAsk` example, for the same reasons

### `get`

This is the same as the `ApplicativeAsk` case. You get the `X` value and mix it to `Y`.

### set

Here we see the first use of `Mixer[X, Y].inject(y, x)`.

What this does is replace all `Y`-atoms in `x`, with the values provided by `y`.

It is equivalent to `Mixer[(X, Y), Y].mix`, and is provided for you to avoid the extra compile time calculating that extra `Mixer`.

In our above problem example using `Y = (B, D)` and `X = (A, B, C, D)`, we replace the `B` and `D` values in `X` with the values from `Y`.

### inspect

This one is again fairly obvious, and we combine the types the only way we can. We project `X` to `Y`, and then apply the
provided `f`.

### modify

Here we use `Mixer[X, Y].modify(f)(x)`, for some `f: Y => Y`.

What `modify` does is lift a function `f: Y => Y` to the larger context `X => X`. That is, it provides a function `X => X` which
has the effect of mutating the `Y`-atoms within `X` according to `f`.

In our above example with `Y = (B, D)` and `X = (A, B, C, D)`, our `f: (B, D) => (B, D)` would be lifted to:

```scala mdoc:compile-only
val g: (A, B, C, D) => (A, B, C, D) = { case (a, b, c, d) =>
  val (b2, d2) = f(b, d)
  (a, b2, c, d2)
}
```

---

Now, taking our original `MonadState`:

```scala mdoc:silent
implicit val monadState: MonadState[F, (A, B, C, D)] = null
```

we can generate any sub-`MonadState`, for free:

```scala mdoc:compile-only
implicitly[MonadState[F, (B, D)]]
implicitly[MonadState[F, (B, A)]]
implicitly[MonadState[F, (C, D, B)]]
implicitly[MonadState[F, B]]
```
