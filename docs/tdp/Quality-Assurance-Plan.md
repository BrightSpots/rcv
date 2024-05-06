# RCTab v1.3.2 Section 12 - Configuration Management Plan v.2.3.2

> **RCTab v1.3.2 Section 12 - Configuration Management Plan v.2.3.2**
> document is solely for use in the State of California. This document
> can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

## 9.1 Configuration management plan

This document describes the Configuration Management Plan for the RCTab
software.

## 9.2 Configuration management policy

RCTab v1.3.2 is a software-only utility. Tools and practices used to
track development of RCTab are described below. In brief, we rely on the
below practices to track and manage development of RCTab:

-   Software tools for version control and update and issue tracking

-   Configuration management policies, as outlined in this document

-   Quality Assurance practices, as described in the QA Document and
    reflected in the GitHub issues tracking software

## 9.3 Configuration identification

RCTab consists of one non-COTS component: The RCTab software itself. A
release of the RCTab software consists of the correctly identified
version of the RCTab software, as identified following **RCTab v1.3.2
Section 22 - Installation Instructions for Windows OS v.1.3.0**.

Any RCTab workstation should include the correct version of the RCTab
software. For this version, that is:

-   RCTab v1.3.2

Any hardware used should conform to the requirements described in
**RCTab v1.3.2 Section 03 - System Hardware Specification v.1.1.1**. Any
additional software should conform to the requirements described in this
document and in **RCTab v1.3.2 Section 16 - System Hardening
Procedures - Windows OS v.1.3.0**.

### 9.3.1 and 9.3.2: Software Versioning and Naming conventions

Our versioning conventions follow the conventions for semantic
versioning as described at
[<u>https://semver.org/</u>](https://semver.org/) and
[<u>https://datasift.github.io/gitflow/Versioning.html</u>](https://datasift.github.io/gitflow/Versioning.html).
In brief:

Given a version number MAJOR.MINOR.PATCH-buildNo, we increment:

-   **PATCH** when we fix something

-   **MINOR** when we add a new feature

-   **MAJOR** when we break backwards-compatibility or add major
    features

-   We use the **buildNo** to differentiate different builds off the
    same branch, and to differentiate between development, test and
    production builds.

A major release significantly alters the functionality of RCTab and
changes how the user interacts with RCTab.

A minor release makes small improvements, contains a collection of bug
fixes, or otherwise adds functionality to RCTab without changing the
user experience.

Patch releases fix any critical bugs that must be fixed before a user
can use the RCTab software.

File naming conventions for files produced by RCTab are described in
***RCTab v1.3.2 Section 02 - Software Design and Specifications
v.1.3.1**.*

**Documentation Naming and Versioning**

All documentation in this submission is named according to the formula:

\[Software version\] \[Document Number-\] \[Document Name\] \[Document
version\]

Software Version: RCTab v1.3.2

Document version: 1.0.1

Example:

RCTab v1.3.2 Section 11 - Quality Assurance Plan v.1.0.1

Document names either reference the set of VVSG standards the document
is responsive to or provide a brief description of the purpose or
function of the information included in the document.

## 9.4 Baseline and promotion activities 

The below sections describe how baseline software versions of RCTab are
defined and how documentation baseline versions are defined.

### Establishing a baseline for RCTab

RCVRC & Bright Spots define a baseline for the RCTab software when a
software release version of RCTab is merged and tested following the
configuration control procedures described below.

### Promoting a subsequent version to baseline status & Maintenance Until Retirement

RCVRC & Bright Spots use an internal versioning scheme based upon the
semantic versioning described above. Upon release of a version to the
Voting System Test Lab, Bright Spots gives that version a three-part
version number. If any bug fixes are needed in response to test
findings, the third part of the version number (the PATCH label) is
incremented to reflect the change.

The official version number is updated after a certification process
completes.

Once a baseline is established, Bright Spots configures and incorporates
all code changes according to the configuration control procedures
described below. After code changes are regression tested and all tests
pass, that version of the software is promoted to being the new baseline
version of the software.

We use tags to track issues while they are being worked on, when any
code updates are merged, and after issues are closed. Release notes for
each release of RCTab note any changes made to the software and link out
to the relevant issue for each change in the release. See
[<u>https://github.com/BrightSpots/rcv/releases/</u>](https://github.com/BrightSpots/rcv/releases/)
for examples of how these issues are tracked in release notes.

RCTab will be updated as necessary in the future, following the version
control and source control procedures described in this document, until
such time as the RCTab software itself is retired.

## 9.5 Configuration control procedures

### 9.5(a) Developing and Maintaining Internally Developed Items

We use Git version control software (git-scm.com) in conjunction with
Github (github.com/BrightSpots/rcv) to coordinate the efforts of our
developers and maintain a complete record of ALL software code changes
to RCTab and the reasoning behind them.

All code changes submitted for incorporation into the RCTab software
must undergo a manual code review from at least one developer other than
the original drafter/developer. Other stakeholders and experts are
involved with code reviews as needed. Code reviews offer an additional
opportunity to identify potential issues, improve code structure,
clarity, performance, and robustness. Code merges are blocked until at
least one developer explicitly approves the code submission, at which
point the code is merged, and regression tests will be run. For code
review examples, see: github.com/BrightSpots/rcv/pulls.

Documentation was all drafted in Google Docs, which tracks changes to
all documents through its built-in document history. However, our TDP
documentation was written to address compliance issues and not as a
proactive feature of development. We plan to conduct an audit of all
documentation after certification and plan to create a system for
tracking and updating documentation into the future to ensure compliance
with all requirements. Documents also include a Document Revision
History box at the bottom of each document which notes dates of
revision, describes any revisions made, tracks authors, and provides
document version numbers. Document versioning will follow the versioning
described above.

### 9.5(b) Acquiring and maintaining third party items

Third-party items included in the RCTab submission can be divided into
two categories:

-   COTS hardware that runs the RCTab software

-   Open-source software artifacts included in the RCTab software

### Hardware

RCTab software runs on COTS computers. Jurisdictions purchase this COTS
hardware directly, based on requirements provided by RCVRC and Bright
Spots. The hardware used for RCTab software is discussed in **RCTab
v1.3.2 Section 03 - System Hardware Specification v.1.1.1**.

### Software

All open-source software libraries and other tools included with RCTab
software can be obtained and updated using the internet and are
maintained by their respective authors unless stated otherwise in that
product’s documentation.

Bright Spots downloads the versions it requires and tracks the software
versions needed for the operation of RCTab.

All third-party software included in RCTab software is unmodified.

Trusted Build instructions and verification steps are available in
**RCTab v1.3.2 Section 14 - Tabulator Trusted Build Instructions
v.1.2.2.**

The document **RCTab v1.3.2 Section 02 - Software Design and
Specifications v.1.3.1** maintains a record of the software libraries
and artifacts used in the RCTab software. For details on the software
tools used to create RCTab, see **RCTab v1.3.2 Section 02 - Software
Design and Specifications v.1.3.1**.

### Additional COTS Software

Any RCTab workstation should also include the following COTS software:

-   Windows 10 Pro, or above

-   LibreOffice

-   XML Notepad

-   Users must also retain access to:

    -   Command Prompt

    -   Notepad

-   UPS

All COTS software should be obtained from the original provider, as
described in **RCTab v1.3.2 Section 16 - System Hardening Procedures -
Windows OS v.1.3.0.**

### 9.5(c) & (d) Resolving internally and externally identified defects

Any defects discovered through testing or reported by users are recorded
and tracked using Github issue tracking tools. We confirm the existence
of the defect, evaluate its severity, and mark it accordingly. Depending
on user impact, development resources, and release timelines, we
schedule developers to address the defects in order of priority and then
do the actual work. Typically, this involves:

1.  reproducing the defect;

2.  making necessary code changes to fix the defect on an isolated git
    branch;

3.  requesting a code review and implementing any changes arising from
    the code review

4.  verifying that regression tests pass;

5.  pushing the code change to the main branch and linking the issue in
    the commit message; and

6.  closing the issue and linking to the commit in Github issue tracker.

In this way, we are able to ensure that all known issues are tracked,
and the most important ones are mitigated according to criticality. For
more details, see:
[<u>https://github.com/BrightSpots/rcv/issues</u>](https://github.com/BrightSpots/rcv/issues).

## 9.6 Release process

This section describes how RCTab is released to test labs and end users.

### 9.6.1 First release to an accredited test lab

RCVRC submits all RCTab artifacts necessary for testing to state
election officials, or the relevant election officials. Those officials
then submit materials to the test lab.

When RCTab is ready to download, RCVRC & Bright Spots sends an email to
state election officials or test lab representatives. That email will
contain a link to a downloadable version of the most recent build of
RCTab and a SHA-512 hash code for the RCTab build shared. Downloads will
either be provided through a Google Drive link or via the RCTab releases
page on GitHub (https://github.com/brightspots/rcv). This SHA-512 hash
code can be used to validate the version of the RCTab downloaded using
the download link by following the instructions in ***RCTab v1.3.2
Section*** ***23 - HashCode Instructions - Windows OS v.1.2.1***.

After downloading and verifying files, the test lab will install the
software as described in ***RCTab v1.3.2 Section 22 - Installation
Instructions for Windows OS v.1.3.0.***

### 9.6.2 Upgrade Release Scenario

In the event that any upgrades or changes are made to the RCTab system,
the RCVRC & Bright Spots will send a new download link as described
above to download a new version of RCTab. Any previous versions of RCTab
on hardware being used by the test lab will need to be uninstalled,
which can be done by deleting RCTab folder and any files created or used
by RCTab (such as configuration .json files, summary result .csv and
.json files, audit .log files, and any CVRs used with RCTab).

### 9.6.3 & 9.6.5 Delivery and Installation of System to Jurisdiction

Jurisdictions will obtain the Trusted Build version of RCTab from the
relevant State Election Officials on a flash drive or some other form of
digital media. ***RCTab v1.3.2 Section 22 - Installation Instructions
for Windows OS v.1.3.0*** explains how to install and verify RCTab. The
RCTab version installed is also included in the operator log box at the
bottom of the RCTab user interface.

### 9.6.4 Maintenance or Upgrade Release of System to Jurisdiction

RCTab upgrades will be released to jurisdictions after they have gone
through necessary testing by state officials and test labs. Releases
will be provided to jurisdictions as described in 9.6.3.

## 9.8 Configuration management resources

RCTab v1.3.2 is a software-only utility. The manufacturer uses Git as
its distributed version-control system to track changes in the source
code during software development. It is designed for coordinating work
among programmers and is used to track changes in any set of files. Its
goals include speed, data integrity, and support for distributed,
non-linear workflows. Here is a link to additional Git information:
[<u>Git External Documentation</u>](https://git-scm.com/doc/ext)
(https://git-scm.com/doc/ext).

Git may be downloaded free via this link: [<u>Git
Download</u>](https://git-scm.com/) (https://git-scm.com/).

The Bright Spots developers use GitVersion as the tool to achieve
*Semantic Versioning* on the RCTab software development and upgrades.
Additional information is available in ***RCTab v1.3.2 Section 13 -
Quality Assurance Plan v.1.2.2*** and here: [<u>GitVersion
Information</u>](https://datasift.github.io/gitflow/Versioning.html)
([<u>https://datasift.github.io/gitflow/Versioning.html</u>](https://datasift.github.io/gitflow/Versioning.html)).

## 9.11 Configuration audits

The guidelines require two kinds of configuration audits:

-   Physical Configuration Audits (PCA)

-   Functional Configuration Audits (FCA)

### 9.11.1 Physical Configuration Audit

The Physical Configuration Audit is conducted by the S-ATA to compare
the voting system components submitted for certification to the
manufacturer’s technical documentation.

For the PCA, a manufacturer shall provide:

1.  Identification of all items that are to be a part of the software
    release

#### RCTab Software

Any RCTab workstation should include the correct version of the RCTab
software. For this version, that is:

-   RCTab v1.3.2

#### COTS Software

Any RCTab workstation should also include the following COTS software:

-   Windows 10 Pro, or above

-   LibreOffice

-   XML Notepad

-   Users must also retain access to:

    -   Command Prompt

    -   Notepad

-   UPS

All COTS software should be obtained from the original provider, as
described in **RCTab v1.3.2 Section 16 - System Hardening Procedures -
Windows OS v.1.3.0**.

#### RCTab Documentation

For a list of documentation, see **RCTab v1.3.2 Section 00 - Table of
Contents v.1.1.0**. Documentation is also provided on a flash drive
along with the installation .zip for RCTab.

#### Acceptance test procedures and criteria

Acceptance test procedures and criteria are included in the following
section:

-   ***RCTab v1.3.2 Section 05 - Acceptance Test Procedures v.1.2.1***

-   ***RCTab v1.3.2 Section 11 - L&A Testing v.1.3.2***

-   ***RCTab v1.3.2 Section 17 - System Test & Verification
    Specification v.1.3.1***

-   ***RCTab v1.3.2 Section 22 - Installation Instructions for Windows
    OS v.1.3.0***

There are no changes between the system used for the PCA and the system
used for the FCA.

Materials needed for a configuration audit:

#### Software

<table>
<colgroup>
<col style="width: 30%" />
<col style="width: 26%" />
<col style="width: 42%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Software</strong></th>
<th><strong>Description</strong></th>
<th><strong>SHA 512 Hash Value</strong></th>
</tr>
<tr class="odd">
<th>RCTab v1.3.2</th>
<th>RCTab Software</th>
<th>8614c061d44000da5caf4b73b3b6f3b500161d52d67dbcc8b976f23d4e6ceac5e327ec7c4a2880afe464dd848add881468543cc7a07d37c0af4cea4c70ac5c59</th>
</tr>
<tr class="header">
<th colspan="3">Any RCTab workstation should also include the following
COTS software:</th>
</tr>
<tr class="odd">
<th>LibreOffice 7.5.7</th>
<th>For viewing formatted .csv output</th>
<th>30dd3e7f158d3a6289d474d8d8a90eb8995ff5847aab0b23dd4dcc6455e36374</th>
</tr>
<tr class="header">
<th>UPS</th>
<th></th>
<th rowspan="3"><p>All COTS software should be obtained from the
original provider, as described in RCTab v1.3.2 Section 16 - System
Hardening Procedures - Windows OS v.1.3.0</p>
<p>All COTS software should be obtained from the original provider, as
described in RCTab v1.3.2 Section 16 - System Hardening Procedures -
Windows OS v.1.3.0</p></th>
</tr>
<tr class="odd">
<th colspan="2">Users must also retain access to or be granted access to
built-in Windows 10, or above, components:</th>
</tr>
<tr class="header">
<th><ul>
<li><p>Notepad</p></li>
<li><p>Command Prompt</p></li>
</ul></th>
<th></th>
</tr>
</thead>
<tbody>
</tbody>
</table>

1.  Specification of compiler (or choice of compilers) to be used to
    generate executable programs

    1.  See RCTab v1.3.2 **Section 03- System Hardware Specification
        v.1.1.1** for additional hardware requirements.

2.  Identification of all hardware that interfaces with the software

    1.  See RCTab v1.3.2 **Section 03- System Hardware Specification
        v.1.1.1** for additional hardware requirements.

3.  Configuration baseline data for all hardware that is unique to the
    system

#### RCTab Hardware

RCTab relies on COTS hardware. See also **RCTab v1.3.2 Section 03 -
System Hardware Specification v.1.1.1** for a complete list.

#### **Hardware** 

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 24%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Component Name</strong></th>
<th><strong>Model/Version Number</strong></th>
<th><strong>Operating System</strong></th>
<th rowspan="2"><strong>See <a
href="#rctab-v1.3.2-section-03---system-hardware-specification-v.1.1.1">RCTab
v1.3.2 Section 03 - System Hardware Specification v.1.1.1</a> for
additional hardware requirements.</strong></th>
</tr>
<tr class="odd">
<th>RCTab Workstation</th>
<th>HP Z4 G4 workstation</th>
<th>Windows 10 Pro or above</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

#### Peripherals

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Part Name</strong></th>
<th><strong>Model Number</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>Printer</th>
<th>Samsung Xpress M2020W Printer (or similar)</th>
<th>RCV Printer</th>
</tr>
<tr class="header">
<th>Uninterruptible Power Supply</th>
<th>APC Backup UPS 600 (or similar)</th>
<th>RCV External Power Supply</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

#### Test Support Equipment/Materials

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 20%" />
<col style="width: 45%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Component Name</strong></th>
<th><strong>Quantity</strong></th>
<th><strong>Description</strong></th>
</tr>
<tr class="odd">
<th>8.5 x 11 Printer Paper</th>
<th>As needed</th>
<th>HP 200350</th>
</tr>
<tr class="header">
<th>Flash drive</th>
<th>2+</th>
<th>For transporting cast-vote records and files produced by RCTab</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

1.  Copies of all software documentation intended for distribution to
    users, including program listings, specifications, operations
    manual, voter manual, and maintenance manual

    1.  See also:

> **RCTab v1.3.2 Section 18 - User Guide v.1.2.2**,

1.  User acceptance test procedures and acceptance criteria

    1.  See [**RCTab v1.3.2 Section 05 - Acceptance Test Procedures
        v.1.2.1**](https://docs.google.com/document/d/1cSzY5KTO-u7w1QuiPIU-ZK2Sv_ExojtS/edit#heading=h.tkfg5gstznzu)

2.  Identification of any changes between the physical configuration of
    the system submitted for the PCA and that submitted for the FCA,
    with a certification that any differences do not degrade the
    functional characteristics

    1.  See also:

> **RCTab v1.3.2 Section 08 - System Operations Procedures v.1.2.2**,
> **RCTab v1.3.2 Section 09 - System Maintenance Manual v.1.3.2**,
> **RCTab v1.3.2 Section 15 - System Change Notes v.1.2.1**

1.  Complete descriptions of its procedures and related conventions used
    to support this audit by:

    1.  Establishing a configuration baseline of the software and
        hardware to be tested

        1.  One Computer with RCTab installed. Installation instructions
            can be found in RCTab v1.3.2 Section 22 - Installation
            Instructions for Windows OS v.1.3.0 document.

        2.  Recommended battery backup if the user jurisdiction facility
            does not have generator capabilities in the event of a power
            failure.

        3.  Verify that all hardware components of the RCTab computer
            are turned on and functioning properly. This includes checks
            of the printer(s), flash drives, USB port, etc.

        4.  Turn on the RCTab computer. As the computer boots up, verify
            that the correct versions of the operating system and the
            RCTab software are installed. Compute the hash codes for the
            RCTab voting system software and compare them with the hash
            value provided with the trusted build.

    2.  Confirming whether the system documentation matches the
        corresponding system components

        1.  Documentation is broken down into different sections to make
            identification of a specific procedure easier for the user.
            Documentation uses language directly from the RCTab software
            that the user would see as well as including images where
            applicable. Additionally, the **RCTab v. 1.3.1 Section 08 -
            System Operations Procedures v. 1.2.1** and **RCTab v1.3.2
            Section 18 - User Guide v.1.2.2** sections provide
            step-by-step instructions for the user with reference to
            other sections for additional details as needed.

### 9.11.2 Functional Configuration Audit

The Functional Configuration Audit is conducted by the S-ATA to verify
that the system performs all the functions described in the system
documentation. The manufacturer shall:

1.  Completely describe its procedures and related conventions used to
    support this audit for all system components

2.  Provide the following information to support this audit:

    1.  Copies of all procedures used for module or unit testing,
        integration testing, and system testing

    2.  Copies of all test cases generated for each module and
        integration test, and sample ballot formats or other test cases
        used for system tests

    3.  Records of all tests performed by the procedures listed above,
        including error corrections and retests

All test cases for RCTab are available at
[<u>https://github.com/BrightSpots/rcv/tree/master/src/test/resources/network/brightspots/rcv/test\_data</u>](https://github.com/BrightSpots/rcv/tree/master/src/test/resources/network/brightspots/rcv/test_data).
Test and Verification requirements are also described in:

-   **RCTab v1.3.2 Section 05 - Acceptance Test Procedures v.1.2.1**

-   **RCTab v1.3.2 Section 11 - L&A Testing v.1.3.2**

-   **RCTab v1.3.2 Section 17 - Test & Verification Specifications
    v.1.3.1**

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 11%" />
<col style="width: 48%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><strong>Document Revision History</strong></th>
</tr>
<tr class="odd">
<th><strong>Date</strong></th>
<th><strong>Version</strong></th>
<th><strong>Description</strong></th>
<th><strong>Author</strong></th>
</tr>
<tr class="header">
<th>10/11/2023</th>
<th>2.3.2</th>
<th>Update RCTab Workstation model, hash value of v1.3.2, required
software</th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>04/28/2023</th>
<th>2.3.1</th>
<th>Updated to Reflect RCTab v1.3.1</th>
<th>Sam Prescott</th>
</tr>
<tr class="header">
<th>04/18/2023</th>
<th>2.3.0</th>
<th>Changed to reflect CA Voting System Standards</th>
<th>Ryan Kirby</th>
</tr>
<tr class="odd">
<th>04/04/2023</th>
<th>2.2.0</th>
<th>Updated to Reflect RCTab v.1.3.0</th>
<th>Rene Rojas</th>
</tr>
<tr class="header">
<th>01/12/2022</th>
<th>2.1.0</th>
<th>Updated URCVT to RCTab and removed NY.</th>
<th>Rene Rojas</th>
</tr>
<tr class="odd">
<th>05/09/2021</th>
<th>2.0.0</th>
<th>Creation of full Configuration Management Plan document</th>
<th>Chris Hughes</th>
</tr>
<tr class="header">
<th>04/21/2021</th>
<th>1.0.0</th>
<th>Initial version</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 

