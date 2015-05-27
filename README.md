# nuxeo-external-reference
===

This plugin enables to store external references to nuxeo using automation operations. The use case if for external systems like portals that reference Nuxeo Documents and want to notify Nuxeo of the usage. So that Nuxeo knows that the document is being used in a external system.

One important point is the fact that the document referenced can be either a normal Document or a proxy Document (for published document in Sections for instance). If a Document proxy is stored, the plugin will also store the reference to the live Document.

3 Automation Operations are available to Add, Get, and remove references.

A widget template for document tabs is available, it will display what are the external references of the current document or its published version.

Unit tests give usage examples.

The user doing the updates must be either in of of the groups "members" or "external" (the later is not a default nuxeo group). The user must have read permission of the document being referenced.


Compile with: mvn install




## License
(C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
Frederic Vadon 

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com) and packaged applications for Document Management, Digital Asset Management and Case Management. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.
