package com.rcv;

import java.util.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

// list of contest rankings for a given contest, i.e. a Ballot entry
@JsonDeserialize(using = ContestRankings.ContestRankingsDeserializer.class)
public class ContestRankings {
  List<ContestRanking> rankings = new LinkedList<ContestRanking>();

  public ContestRankings(List<ContestRanking> rankings) {
    this.rankings = rankings;
  }

  static class ContestRankingsDeserializer extends JsonDeserializer<ContestRankings> {
    @Override
    public ContestRankings deserialize(
      JsonParser jsonParser,
      DeserializationContext deserializationContext
    ) throws IOException {
      final ContestRanking[] array = jsonParser.readValueAs(ContestRanking[].class);
      return new ContestRankings(Arrays.asList(array));
    }
  }
}
