---
layout: home
title:  "Home"
---

# Alphabet Soup

### Typelevel Transformations

Alphabet Soup is a boilerplate-reduction library that will generate mappings between data-structures for you, automatically.

The order of types in a tuple or case class should not matter, just the type itself, if all the types are unique:

```scala
type Source = (A, B, (C, D), (E, (F, G)))
type Target = (G, A, (E, F), B, (D, C))
```

If you have a `Source`, you can always produce a `Target`. Alphabet Soup will give you any such mapping, for free.

It works on case classes, tuples, hlists, and any arbitrarily nested combination of all three.
