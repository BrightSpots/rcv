package com.rcv;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = ContestRanking.ContestRankingDeserializer.class)
public class ContestRanking {

  int optionId;
  int ranking;

  ContestRanking(int ranking, int optionId) {
    this.ranking = ranking;
    this.optionId = optionId;
  }

  public int getOptionId() {
    return optionId;
  }

  public int getRanking() {
    return ranking;
  }

  static class ContestRankingDeserializer extends JsonDeserializer<ContestRanking> {
    @Override
    public ContestRanking deserialize(
      JsonParser jsonParser,
      DeserializationContext deserializationContext
    ) throws IOException {
      final Integer[] array = jsonParser.readValueAs(Integer[].class);
      return new ContestRanking(array[0], array[1]);
    }
  }
}
