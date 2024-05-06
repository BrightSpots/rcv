# RCTab v1.3.2 Section 07J - Security Architecture v.1.0.2

# 

> **RCTab v1.3.2 Section 07J - Security Architecture v.1.0.2** document
> is solely for use in the State of California. This document can be
> expanded or updated as is necessary or required. Where relevant, this
> document refers to specific sections and requirements of the
> California Voting System Standards. Any recommendations listed in this
> document should not supersede user jurisdiction procedures or other
> controlling governance entities.

**Objective:** This document shall provide an architecture level
description of how the security requirements are met, and shall include
the various authentication, access control, audit, confidentiality,
integrity, and availability requirements.

**Authentication:** Access to RCTab should be at minimum, made by no
less than two employees within the user jurisdiction. These employees
should have received the suggested training time provided by the
manufacturer before accessing the software. See also
**<span class="mark">RCTab v1.3.2 Section 10 - Personnel Deployment and
Training v.1.2.1</span>.** Access to the desktop or laptop should
require password entry from the initial operating system for all users
assigned to operate RCTab. See also **<span class="mark">RCTab v1.3.2
Section 16 - System Hardening Procedures - Windows OS v.1.3.0</span>**
document for more information.

**Access Control: <span class="mark">RCTab v1.3.2 Section 16 - System
Hardening Procedures - Windows OS v.1.3.0</span>** enumerates steps for
creating a ‘RCTab’ Windows Standard user account on the RCTab machine.
Installation instructions for RCTab in **RCTab v1.3.2 Section 22 -
Installation Instructions for Windows OS v.1.2.2** describe how to set
up and run the RCTab software using the ‘RCTab’ user account which has
the absolute minimum permissions necessary.

Users should only access the software per jurisdiction approved rules
and after users have obtained the recommended training as outlined in
**<span class="mark">RCTab v1.3.2 Section 10 - Personnel Deployment and
Training v.1.2.1</span>**. See also: **<span class="mark">RCTab v1.3.2
Section 07 - System Security Specification Requirements
v.1.4.0</span>.**

Further description of the two Windows OS Accounts and they offer access
control can be found in **RCTab v1.3.2 Section 07 - System Security
Specification Requirements v.1.4.0** under headings
[<u>9.6.1</u>](#access-control) and
[<u>9.6.1.2</u>](#access-control-measures).

**Audit:** RCTab produces audit logs and tabulator operator logs.
Logging capabilities are described in the RCTab Logging section of
**<span class="mark">RCTab v1.3.2 Section 02 - Software Design and
Specifications v.1.4.2</span>**. Detailed documentation describing how
to read and make use of these logs is provided in
**<span class="mark">RCTab v1.3.2 Section 28 - Post-Election Audit &
Clearing RCTab from System v.1.1.1</span>**. Windows OS also logs all
events on the OS. Those event logs are available via the Windows Event
Log application.

**  
Confidentiality:** While RCTab does use cast vote records to tabulate
results, those records do not contain any voter-identifying information.
Use of RCTab should only be performed by trained personnel. See also
**<span class="mark">RCTab v1.3.2 Section 10 - Personnel Deployment and
Training v.1.2.1</span>.**

**Integrity:** See **<span class="mark">RCTab v1.3.2 Section 03 - System
Hardware Specification v.1.1.1</span>** for minimum operating
specifications and **<span class="mark">RCTab v1.3.2 Section 16 - System
Hardening Procedures - Windows OS v.1.3.0</span>** for procedures to
ensure the hardware is adequately protected against unauthorized access,
theft of data, and/or malicious attacks. Following any maintenance or
replacement of equipment used to operate RCTab, users should refer to
**<span class="mark">RCTab v1.3.2 Section 05 - Acceptance Test
Procedures v.1.2.1</span>.** Integrity of CVR inputs as well as summary
file and audit log outputs is addressed in RCTab v1.3.2 Section 07 -
System Security Specification Requirements v.1.4.0

**Availability:** RCTab is used on COTS equipment. While equipment
failure is rare, it should be recognized as a possibility. Jurisdiction
backup and disaster plans should include strategies for handling
equipment failures and replacements before they occur. See
**<span class="mark">RCTab v1.3.2 Section 03 - System Hardware
Specification v.1.1.1</span>** for minimum operating specifications and
**<span class="mark">RCTab v1.3.2 Section 16 - System Hardening
Procedures - Windows OS v.1.3.0</span>** for procedures to ensure the
hardware is adequately protected against unauthorized access, theft of
data, and/or malicious attacks. Following any maintenance or replacement
of equipment used to operate RCTab, users should refer to
**<span class="mark">RCTab v1.3.2 Section 05 - Acceptance Test
Procedures v.1.2.1</span>.** The manufacturer also recommends conducting
a post-installation and post-election hashing as outlined in
**<span class="mark">RCTab v1.3.2 Section 23 - HashCode Instructions -
Windows OS v.1.2.1</span>.**

<table>
<colgroup>
<col style="width: 14%" />
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
<th>09/27/2023</th>
<th>1.0.2</th>
<th>Include 1.3.2 updates in Access Control and Integrity sections</th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>04/18/2023</th>
<th>1.0.1</th>
<th>Updated to reflect RCTab v1.3.1</th>
<th>Rosemary Blizzard</th>
</tr>
<tr class="header">
<th>04/18/2023</th>
<th>1.0.0</th>
<th>Security Architecture Requirements</th>
<th>Rosemary Blizzard</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

#  

