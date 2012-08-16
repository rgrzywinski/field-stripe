# Efficient Field-Striped, Nested, Disk-backed Record Storage

## Description

Field-stripe encoding similar in spirit to that of Google's [Dremel](http://research.google.com/pubs/pub36632.html).
As is common in many publications, the "what" and "why" were omitted for lack of
time and space from the Dremel paper. We wanted to understand the requirements, 
assumptions, trade-offs, etc. necessary for creating a field-stripe based encoding
and compare that with the results / implementation presented in the Dremel paper. 

The primary artifact is the document [Efficient Field-Striped, Nested, Disk-backed Record Storage](https://github.com/rgrzywinski/field-stripe/blob/master/Field-Striped_Nested_Storage.pdf).
The document is by no means complete and is in desperate need of a re-write but
it is packed with information. In order to give the document some teeth and to
understand the implications of what it proposes, a simple Java-based implementation
was crafted.

## Notes

The software has been updated to use [Protostuff](http://code.google.com/p/protostuff/)
as a parser.

## Conclusion

It is our hope that this work, although incomplete, can be used to "prime the 
pump" for an open source implementation of a field-stripe backed system (such 
as [Drill](http://wiki.apache.org/incubator/DrillProposal)).
