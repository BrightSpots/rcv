package com.rcv;

import java.util.List;

// container for a Contest:
// has an id and a list of "options" i.e. candidates competing for this office
// TODO: extend this to support configuring contests to be rcv or plurality
public class Contest {
  // the name of this contest i.e. "2018 Governor"
  public String name;
  // unique identifier
  public int id;
  // all candidates competing for this office
  public List<String> options;

  public Contest() {}

}
