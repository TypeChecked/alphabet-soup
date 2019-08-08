---
layout: docs
title: Introduction
---

# Introduction

Alphabet soup provides mappings between arbitrarily nested case classes, tuples and hlists - for free.

The top-level class you will be using is `Mixer[Source, Target]`, which produces a mapping from `Source` to `Target`. Here's a quick example:

```scala
case class Source(tuple: (A, B, C), hlist: D :: (E, F) :: HNil)
type Target = (B, F, A, D)

val source: Source = ???

val target: Target = Mixer[Source, Target].mix(source)
```

As long as all the types on the right, in `Target`, are available on the left, in `Source`, then the mixer will compile
and will generate your mapping.

Unlike some existing data transformation solutions, alphabet soup is _not_ name-based.
It matches the types purely on type equality and not their nested depth, field name, or relative position.

This approach has benefits and allows more flexible applications, but also introduces some restrictions.

Please have a read about [atoms](/docs/atoms.html) before you begin using the library.
