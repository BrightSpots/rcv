package com.rcv;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = ContestRanking.ContestRankingDeserializer.class)
public class ContestRanking {

  String optionId;
  int rank;

  ContestRanking(int rank, String optionId) {
    this.rank = rank;
    this.optionId = optionId;
  }

  public String getOptionId() {
    return optionId;
  }

  public int getRank() {
    return rank;
  }

  static class ContestRankingDeserializer extends JsonDeserializer<ContestRanking> {
    @Override
    public ContestRanking deserialize(
      JsonParser jsonParser,
      DeserializationContext deserializationContext
    ) throws IOException {
      final Object[] array = jsonParser.readValueAs(Object[].class);
      return new ContestRanking((Integer)array[0], (String)array[1]);
    }
  }
}
