## RCTab Contributor Guide

---

Thank you for your interest in RCTab.  We are one of only two open source projects used to produce election results in the United States.  The other is [Voting Works](https://www.voting.works/).  In 2021, fourteen cities across the country (including New York City) used RCTab in their elections.

The Ranked Choice Voting Resource Center, a nonpartisan 501(c)(3), and the Bright Spots team of volunteer programmers, provide this software and support for free, directly to jurisdictions and individuals interested in implementing ranked-choice elections.  The RCVRC also provides flexible licensing to commercial vendors wishing to use RCTab with their election systems.  This keeps RCTab open source and helps offset development and support costs.  

### How to make a contribution:

1. If you’re interested in contributing code please fill out a copy of this [Contributor Release Form](https://docs.google.com/document/d/1lTpwjQBnS7u8ONdm1rg7nhzXBB8o_I1n/edit?usp=sharing&ouid=116743718777594585909&rtpof=true&sd=true) and send it to [chris.hughes@rcvresources.org](mailto:chris.hughes@rcvresources.org).
2. Find or create an issue you want to work on:
- If you know Java: check out the [readme](https://github.com/BrightSpots/rcv#readme) and the list of open [issues](https://github.com/BrightSpots/rcv/issues) in GitHub to see what's on deck.  We plan to label issues which are good for new contributors. 
- If you know Python or JavaScript: consider contributing to our partner project [RCVis](https://github.com/artoonie/rcvis#readme).  It is the best open source RCV results visualizer.  See the [top requested features](https://rcvis.com/#lookingfor) and contact team@rcvis.com for more information.
- For non-coders we have various opportunities to help with [documentation and testing](https://github.com/BrightSpots/rcv/issues?q=is%3Aissue+is%3Aopen+label%3Adocumentation).
- You can also [propose a new feature](https://github.com/BrightSpots/rcv/issues/new) or split an existing issue into multiple chunks of work.
3. Assign the issue to yourself and comment on it.  Please include Rene Rojas @rrojas350 and ask any questions you have.  This keeps any technical conversation tracked and allows everyone to get on the same page with all the details, before writing any code.
4. Write code:
- We adhere to the EAC's [VVSG V1.0](https://github.com/BrightSpots/rcv/blob/develop/reference/VVSG/VVSG-2005.1.0.VOL.1.pdf) software coding standards from section _5.2 Software Design and Coding Standards_.  We recommend reading this.  
- We use an Intellij [inspection tool](https://github.com/BrightSpots/rcv/blob/develop/.idea/inspectionProfiles/Project_Default.xml) to enforce nested scope limits required by the VVSG. 
- We use [Google Java Style](https://google.github.io/styleguide/javaguide.html).  Intellij has a [plugin](https://checkstyle.sourceforge.io/google_style.html) for it which we recommend.
- [Fork our repo](https://docs.github.com/en/get-started/quickstart/contributing-to-projects) and use the [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) branching model.
- All commit messages should hash-reference the issue number in the text e.g. "Don't log config file contents #520"
5. Run the Gradle "check" target, which runs code / style checks, and the regression tests.  This ensures your changes do not break existing code.
6. If you're using IntelliJ, please check all the boxes under the "Commit Checks" section (e.g. Reformat code, Rearrange code, etc.) when creating commits.
7. Create a pull request and make changes suggested by the reviewers.  If the suggested changes significantly expand the scope of an issue, consider breaking them out into a new issue.
8. Merge your pull request and celebrate!  It's a lot of work and we really appreciate your contribution.

If you have any questions or feedback please reach out to Chris Hughes by email at chris.hughes@rcvresources.org.  

- The RCTab Team

     
