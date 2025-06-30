# WorldPainter Code Style
## Sun Code Style
Where not explicitly overridden below, the [Sun/Oracle Java Code Conventions](https://www.oracle.com/docs/tech/java/codeconventions.pdf) apply. These are the default/fallback conventions.

This means, for example:

* Class/interface names start with a capital letter. All class and interface member names start with a lowercase letter, except constants, which should be in all caps and underscores.
* Always use curly braces around blocks, even for single statements.
* Curly braces go on the same line as the `if/else/switch/etc.` expression.
* TODO

## Exceptions
* The unit of indentation is four spaces. No tabs must be used.
* The maximum line length is _roughly_ 120 characters, but readability comes before strict adherence to this rule.
* Switch cases may be indented an additional level.
* TODO

## Additional Rules
* Use redundant parentheses. _Rationale: the order of precedence is sometimes surprising or confusing in Java; using redundant parentheses leaves no room for confusion and prevents errors._
* Write self-documenting code; meaning that identifier names should be informative, logical expressions should be laid out so that they make sense to a human reader, etc. When possible, the code should be written such that it is not necessary to add a comment explaining what it does.
* Don't use `this` when not necessary.
* TODO

## Comments
* All public methods and fields, as well as any methods intended to be overridden, must have Javadoc.
* Code comments must be used to explain _why_ code is doing what it is doing, not _what_ it is doing. The _what_ should already be clear from the code itself (see "write self-documenting code" above).

## Scope Order
Inside a class, the members go in the following order:

1. Constructors
2. Instance methods
3. Static methods
4. Instance fields
5. Static fields
6. Inner class and interface definitions

Within each category, the scopes should be ordered as follows:

1. Public
2. Protected
3. Package private
4. Private

_Rationale: the public API of a class is the most relevant for someone reading the source code and should therefore come first. Private methods, as well as fields and other state, are implementation details which should come later._

## Error Handling
Code should throw exceptions when something goes wrong. Don't swallow exceptions and don't continue as if nothing happened if a fatal error occurred. The exception should be informative and give relevant context information in the exception message. Use `MDCWrappingRuntimeException`, `MDCCapturingRuntimeException`, `MDCWrappingException` or `MDCCapturingException` so that the MDC logging context is captured for extra debug information.

## Existing Code
Not all existing code adheres to all the style rules for historical reasons. Don't change the formatting of existing code, unless you are changing a significant portion of a file, then you can fix the formatting of the entire file to comply with the rules.

## Source File Encoding
All files should use UTF-8 encoding (_without_ a byte order mark).