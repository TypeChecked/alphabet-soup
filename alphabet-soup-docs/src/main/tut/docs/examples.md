---
layout: docs
title: Examples
---

# Worked Examples

```scala mdoc:invisible
import io.typechecked.alphabetsoup._
import io.typechecked.alphabetsoup.macros._
import cats.mtl.ApplicativeAsk
import cats.syntax.functor._
import cats.Functor

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

### The problem

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
