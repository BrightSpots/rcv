# RCTab v1.3.2 Section 07M - Security Testing and Vulnerability Analysis v.1.0.2

> **RCTab v1.3.2 Section 07M - Security Testing and Vulnerability
> Analysis v.1.0.2** document is solely for use in the State of
> California. This document can be expanded or updated as is necessary
> or required. Where relevant, this document refers to specific sections
> and requirements of the California Voting System Standards. Any
> recommendations listed in this document should not supersede user
> jurisdiction procedures or other controlling governance entities.

**Objective:** This document shall describe security tests performed to
identify vulnerabilities and the results of the testing. This also
includes testing performed as part of software development, such as
unit, module, and subsystem testing.

-   We have developed a suite of 68 Tabulation Regression Tests and
    continue to add more tests as new features and bug fixes are added.
    These are designed to verify that various aspects of Tabulator
    functionality behave as expected. They also verify that new code
    changes do not inadvertently alter Tabulator behavior. The entire
    test suite must be run, and all tests must pass before any new code
    changes can be merged into the main Tabulator repository. See
    **<span class="mark">RCTab v1.3.2 Section 17 - System Test and
    Verification Specification v.1.4.2</span>** for additional
    information.

-   Previous VSTL tests with Pro V&V conducted security regression
    reviews of RCTab versions 1.0.1, 1.1.0, and 1.2.0, all of which
    incorporated the security policies of the baseline system. Those
    tests found RCTab to have an applied level of security compliant
    with the verified VVSG 1.0 security provisions. Testing also found
    that RCTab does not impact the ability of the baseline system as
    modified to satisfy the VVSG 1.0 security requirements.

    -   These tests also concluded that hash code procedures during the
        installation process and post-election process would help verify
        that the RCTab software as installed matches the trusted build
        of the software. Se**e RCTab v1.3.2 Section 22 - Installation
        Instructions for Windows OS v.1.3.0** and **RCTab v1.3.2 Section
        23 - HashCode Instructions - Windows OS v.1.2.1** for more.

-   A March 2021 test with SLI found that, “other than some high level
    access control assertions, RCVRC depends heavily on the security
    policies of the accompanying voting system, as well as the security
    policies of local jurisdictions.” Voting System Test Lab report
    results in early 2023 also found the need for enhanced security
    control. In response, version 1.3.2 was released with the following
    updates

    -   **<span class="mark">RCTab v1.3.2 Section 16 - System Hardening
        Procedures - Windows OS v.1.3.0</span>** enumerates steps for
        creating a ‘RCTab’ Windows Standard user account on the RCTab
        machine. Installation instructions for RCTab in **RCTab v1.3.2
        Section 22 - Installation Instructions for Windows OS v.1.2.2**
        describe how to set up and run the RCTab software using the
        ‘RCTab’ user account which has the absolute minimum permissions
        necessary. Following these instructions ensures the following

        -   RCTab users cannot edit or delete RCTab summary output files
            or audit logs.

        -   RCTab users cannot edit or delete corresponding .hash files
            that can be used to verify the contents of all output files
            and audit logs.

    -   When tabulation begins, RCTab automatically, programmatically
        verifies the cryptographic signature of all Hart CVRs used as
        input for contest tabulation. This verification step ensures
        both the integrity (CVR contents have not been edited) and
        provenance (CVRs came from the Hart voting system) of the Hart
        CVRs. RCTab will throw a halting error and tabulation will not
        begin if cryptographic validation of Hart’s CVR signature is not
        successful.

-   See **RCTab v1.3.2 - Section 07L - Security Threat Analysis
    v.1.0.1** for additional analysis of potential threats to the RCTab
    software.

**Document Revision History**

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 11%" />
<col style="width: 48%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Date</strong></th>
<th><strong>Version</strong></th>
<th><strong>Description</strong></th>
<th><strong>Author</strong></th>
</tr>
<tr class="odd">
<th>10/25/2023</th>
<th>1.0.2</th>
<th>Include all 1.3.2 updates to describe how initial testing report
results have all been addressed</th>
<th></th>
</tr>
<tr class="header">
<th>04/28/2023</th>
<th>1.0.1</th>
<th>Updated to reflect RCTab v1.3.1</th>
<th>Kelly Sechrist</th>
</tr>
<tr class="odd">
<th>04/26/2023</th>
<th>1.0.0</th>
<th>Security Testing and Vulnerability Analysis</th>
<th>Chris Hughes</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 

