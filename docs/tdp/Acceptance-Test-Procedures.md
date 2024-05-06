# RCTab v1.3.2 Section 04 - System Functionality Description v.1.1.1

> **RCTab v1.3.2 Section 04 - System Functionality Description v.1.1.1**
> document is solely for use in the State of California. This document
> can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

RCTab is developed to run using the Java Development Kit (JDK) version
17.0.2. The Java platform includes an execution engine, a compiler, and
a set of libraries used by RCTab. For more information, see **RCTab
v1.3.2 Section 02 - Software Design and Specifications v.1.4.2.**

RCTab is designed to run as a stand-alone software compatible with
Windows Operating System. Java 17 compatibility and system requirements
are listed in the [<u>Java 17 Compatibility
Document</u>](https://www.oracle.com/java/technologies/javase/products-doc-jdk17certconfig.html).

RCTab is normally received by the user election jurisdictions as a
fully-compiled program in a Trusted Build by contacting the relevant
authority directly for directions and procedures to receive the Trusted
Build.

Git is the distributed version-control system used to track changes in
the source code during software development. It is designed for
coordinating work among programmers and is used to track changes in any
set of the files. Its goals include speed, data integrity, and support
for distributed, non-linear workflows. Here is a link to additional Git
information: [<u>Git External
Documentation</u>](https://git-scm.com/doc/ext).

**Hardware Functionality and Recovery Requirements**

a\. Restoration of the device to the operating condition existing
immediately prior to an error or failure, without loss or corruption of
voting data previously stored in the device

RCTab relies upon exportable data from a jurisdiction’s voting system.
Data operated upon by RCTab may be stored on external devices such as
flash drives or copied directly into the memory of the hardware RCTab
software is installed on. If RCTab fails while tabulating results, the
original files used should not be altered because the Tabulator does not
directly operate upon CVR data. However, any loss of data caused by a
failure of RCTab or the hardware upon which RCTab is installed should be
rectified by exporting a new copy of the same data from the
jurisdiction’s voting system.

If the hardware RCTab software is installed upon fails while tabulating
results, a user should first restart the machine and RCTab and run the
tabulation again. There is no persistent state of RCTab that would
persist after a crash or after a machine restart.

Original versions of voting data used in RCTab should be maintained on
the jurisdiction’s voting systems for as long as required by
jurisdiction rules. See **RCTab v1.3.2 Section 06 - System Design
Specifications v.1.1.1** and **RCTab v1.3.2 Section 08 - System
Operations Procedures v.1.2.2** for more.

b\. Resumption of normal operation following the correction of a failure
in a memory component, or in a data processing component, including the
central processing unit.

If the hardware RCTab software is installed upon fails while tabulating
results, a user should first restart the machine and RCTab and run the
tabulation again. If the hardware continues to fail, users should
consult the documentation included with the hardware to determine any
error resolution steps. See **RCTab v1.3.2 Section 09 - System
Maintenance Manual v.1.3.2** for more.

c\. Recovery from any other external condition that causes equipment to
become inoperable, provided that catastrophic electrical or mechanical
damage due to external phenomena has not occurred

If the hardware that RCTab is installed upon fails while tabulating
results, a user should first restart the machine and RCTab and run the
tabulation again. There is no persistent state of RCTab that would
persist after a crash or after a machine restart. If failure persists,
users should consult the documentation included with the hardware to
determine any error resolution steps. See **RCTab v1.3.2 Section 09 -
System Maintenance Manual v.1.3.2** for more.

**Vote Tabulating Information**

RCTab processes vote tabulation data exported from a jurisdiction’s
voting systems according to the rules as set up in the configuration
file used by the user when operating RCTab. See **RCTab v1.3.2 Section
02 - Software Design and Specifications v.1.4.2** for more.

a\. Monitor system status and generate machine-level audit reports

RCTab provides users system status information through the log box in
the user interface and through log documents (both tabulation audit logs
produced after successful tabulation and the operator log file, which is
available in the /bin folder on Windows computers). Audit logs capture
details of successful tabulations, including all configuration file
details, CVR parsing information, details on how every ballot was
processed in every round of counting, and summary round-by-round
results. The operator log captures all messages produced by RCTab: those
produced during a failed tabulation, messages sent during a successful
tabulation, messages sent while validating configuration files, and any
other messages sent via the log box in the user interface. See **RCTab
v1.3.2 Section 02 - Software Design and Specifications v.1.4.2** for
more about audit reports and system status reports.

b\. Accommodate device control functions performed by polling place
officials and maintenance personnel

Polling place officials will not interact with the RCTab software.
Maintenance personnel should refer to **RCTab v1.3.2 Section 09 - System
Maintenance Manual v.1.3.2** and **RCTab v1.3.2 Section 08 - System
Operations Procedures v.1.2.2** when interacting with RCTab. All device
control functions are laid out in those materials.

c\. Register and accumulate votes

RCTab processes digital cast vote records of ballots as cast by voters.
Part of this requires the accumulation of cast vote record data; this is
done in accordance with configuration requirements as set by users. See
**RCTab v1.3.2 Section 06 - System Design Specifications v.1.1.1** and
**RCTab v1.3.2 Section 08 - System Operations Procedures v.1.2.2** for
more information about how RCTab handles cast vote records.

d\. Accommodate variations in ballot counting logic

RCTab relies upon cast-vote record data from a jurisdiction’s voting
system. CVR data must be adjudicated before being processed by RCTab.
That cast-vote record data will have varying types of ballot data
depending on how voters marked their ballots and how ballots were
adjudicated, including candidate names, overvoted markings, skipped
rankings, undervoted ballots, and write-in markings. RCTab has settings
for users to determine how each of these different types of markings is
processed by the software. RCTab performs no interpretation of voter
marks on ballots, however, as CVR data used in RCTab software will
include only non-ambiguous marks and data representing how voters marked
their ballots. See **RCTab v1.3.2 Section 25 - Configuration File
Parameters v.1.1.1** and **RCTab v1.3.2 Section 08 - System Operations
Procedures v.1.2.2** for more information on the settings available to
users for processing these varying voter marks.

The only voting variation RCTab supports is ranked order voting. RCTab
supports many permutations of ranked order voting (referred to as ranked
choice voting in the documentation). See **RCTab v1.3.2 Section 08 -
System Operations Procedures v.1.2.2** for more information on the
permutations and settings available to users.

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
<th>04/28/2023</th>
<th>1.1.1</th>
<th>Updated for RCTab v1.3.1</th>
<th>Melissa Hall</th>
</tr>
<tr class="odd">
<th>01/12/2022</th>
<th>1.1.0</th>
<th>System Functionality Description</th>
<th>Ryan Kirby</th>
</tr>
<tr class="header">
<th>04/27/2021</th>
<th>1.0.0</th>
<th>System Functionality Description</th>
<th>Louis Eisenberg</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

