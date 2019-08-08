---
layout: docs
title: Type Uniqueness
---

# Type Uniqueness

After atomising both `Source` and `Target`, we traverse `Target` depth-first.

For each atom `T` in `Target`, we search `Source` depth-first for a matching atom of type `T`.

If we find one, we select it and use it to build our value of type `Target`. If we don't find one, we consider
`DefaultAtom` instances.

If we still don't find a suitable value of type `T`, our `Mixer[Source, Target]` does not compile.

---

As you can see, alphabet soup works on type equality rather than field name. This means that if your `Source` has multiple values of the
same type within it, alphabet soup always sends the first instance (depth-first) to `Target`.

Here's an example:

```scala
case class Source(tuple: (String, Int), int: Int, bool: Boolean)
type Target = (Int, Boolean)

val source = Source("hello" -> 10, 17, true)

Mixer[Source, Target].mix(source) == (10, true)
```

`Target` needs an `Int`, and the first `Int` the algorithm comes across searching the atomised `Source` depth-first is `10`,
and so `10` is the one we see in the result.

In some situations, all your constituent atoms are naturally unique types - such as in a `Reader`. In others, they may
not naturally all be distinct.

Imagine the following case:

```scala
@Atomic case class UserId(value: Int)

case class Loan(payee: UserId, recipient: UserId, amount: Int)
case class Debt(user: UserId, debt: Int)

Mixer[Loan, Debt]
```

Here we're trying to work out the debt that `recipient` is in, based on their loan amount. But alphabet soup can't tell:
when you ask `Loan` for a `UserId` you will get `payee` rather than `recipient` - the wrong behaviour.

In such a situation, where there is implicit business logic attached to a field-name which alphabet soup can't see,
you have two choices:

* Either move the business logic from the field name to the type, and introduce `PayeeId` and `RecipientId` or similar,
probably with automatic mappings from `UserId` to make construction simpler. Or,
* Accept that in this case an automatic mapping is not suitable because you have no way to automate the business logic

Alphabet soup works on the principle that types are canonical, and field names are nothing but hints for developers. As useful
as `_1` in a `Tuple2`.
It is as general and shapeless as it is possible to be, and sometimes it is simply the case that business logic can not
be made automatic, or general.

We do not recommend writing the `Mixer` explicitly yourself, since this will not be reflected in other `Mixer`s generated
automatically on top of `Loan` and `Debt`.
