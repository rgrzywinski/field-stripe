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

As it stands, the software *does not compile* as it references internal dependencies.
This is primarily due to the use of AK's internal Protobuf parser. It will take
a bit of work to adapt to another parser such as [Protostuff](http://code.google.com/p/protostuff/).
Until the above work can be performed use the code as a guide for understanding
how an implementation might look and function. We apologize for any difficulty
that this causes.

## Conclusion

It is our hope that this work, although incomplete, can be used to "prime the 
pump" for an open source implementation of a field-stripe backed system (such 
as [Drill](http://wiki.apache.org/incubator/DrillProposal)).
