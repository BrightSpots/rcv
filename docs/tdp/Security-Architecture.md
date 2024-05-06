# RCTab v1.3.2 Section 07I - Design and Interface Specification v.1.0.2

> **RCTab v1.3.2 Section 07I - Design and Interface Specification
> v.1.0.2** document is solely for use in the State of California. This
> document can be expanded or updated as is necessary or required. Where
> relevant, this document refers to specific sections and requirements
> of the California Voting System Standards. Any recommendations listed
> in this document should not supersede user jurisdiction procedures or
> other controlling governance entities.

**Objective:** This document shall identify the threats the voting
system protects against. This document shall provide a high-level design
of the overall voting system and of each voting system component. It
shall also describe external interfaces (programmatic, human, and
network) provided by each of the computer components of the voting
system (examples of components are DRE, Central Tabulator, Independent
Audit machine).

RCTab is an open-source software package that provides post-election
tabulation to determine results in a ranked choice voting election.
RCTab can process data from voting machines that are capable of
exporting cast vote records (CVRs) and tabulate a single-winner or
multi-winner ranked choice voting election according to the rules used
in current state, county, or city election jurisdictions in the United
States.

The RCTab software is designed to tabulate results based on CVR data
from voting system vendors such as ES&S and Hart InterCivic. RCTab does
not rely on any network connections to produce results; it only needs
compatible CVR data and a set of tabulation rules created by the user in
the RCTab UI to produce RCV results.

## System Security

When used with Hart Verity, RCTab 1.3.2 includes the following
programmatic security mechanisms

-   When tabulation begins, RCTab automatically, programmatically
    verifies the cryptographic signature of all Hart CVRs used as input
    for contest tabulation. This verification step ensures both the
    integrity (CVR contents have not been edited) and provenance (CVRs
    came from the Hart voting system) of the Hart CVRs. RCTab will throw
    a halting error and tabulation will not begin if cryptographic
    validation of Hart’s CVR signature is not successful. This is to
    protect against tampering of CVRs between Hart Verity export and
    RCTab import by malicious internal actors.

-   After successfully following installation instructions in **RCTab
    v1.3.2 Section 22 - Installation Instructions for Windows OS
    v.1.3.0** *only* the RCTab software is run with administrator
    privileges, the logged in Windows user does **not** have
    administrative privileges. All RCTab output files (audit logs,
    summary .csv and .json, corresponding .hash files) are
    programmatically set to Read-Only and cannot be edited by the RCTab
    Windows Standard user.  
      
    This is to protect against any tampering with the content of the
    summary files or audit log by malicious internal actors. This also
    ensures that the tabulation user does not have admin-level OF
    privileges to make changes that could be security issues e.g.
    disabling read-only access or enabling network adaptors.

-   Additionally, RCTab automatically, programmatically creates a
    cryptographic hash of all output files - audit logs, summary .csv
    and .json - that can be used to validate that those files have not
    been edited. This is to protect against any tampering with the
    content of the summary files and audit log.

The objective of these programmatic and procedural security improvements
is to make the execution of internal malicious tampering so onerous that
it will not be a viable target. The combination of procedural and new
programmatic controls require significantly more time and complexity for
any malicious actor - this makes RCTab a less appealing target.
Additionally, these procedural and programmatic controls make auditing a
tabulation to ensure secure, successful execution easier and quicker.

There are still security threats that exist. Certain programmatic
requirements continue to require procedural steps. For example, properly
setting read-only permissions on RCTab output folders and proper account
credential security for the Windows Admin user account. By following the
detailed installation instructions and initial configuration
instructions in **RCTab v1.3.2 Section 22 - Installation Instructions
for Windows OS v.1.3.0** and **RCTab v1.3.2 Section 16 - System
Hardening Procedures - Windows OS v.1.3.0** these threats can be
addressed.

RCTab is meant to be run on an air-gapped, non-internet-connected
computer. This protects against any network-based attacks on the
software. Any RCTab workstation should also be hardened against physical
attacks by physically securing and/or shutting down access to any
unnecessary ports on the RCTab workstation hardware. RCTab workstations
should also be encrypted, physically secured under lock and key when not
in use, and require a username and login to access the OS where RCTab is
installed. This is to protect against a malicious internal actor who
might attempt to connect to the internet to install malicious
software/code.

RCTab relies on a human-computer interface to create tabulation rules
configuration files and run any round-by-round tabulation. RCTab does
not rely on any network or programmatic interfaces.

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
<th>10/25/2023</th>
<th>1.0.2</th>
<th><ul>
<li><p>Includes all security enhancements for 1.3.2 and describe the
threat that they protect against</p></li>
<li><p>Added explicit system security objectives and known
vulnerabilities</p></li>
</ul></th>
<th>Mathew Ruberg</th>
</tr>
<tr class="odd">
<th>04/28/2023</th>
<th>1.0.1</th>
<th>Updated to reflect RCTab v1.3.1</th>
<th>Kelly Sechrist</th>
</tr>
<tr class="header">
<th>04/26/2023</th>
<th>1.0.0</th>
<th>Design and Interface Specification</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

#  

