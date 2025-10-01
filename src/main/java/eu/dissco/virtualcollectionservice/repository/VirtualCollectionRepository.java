package eu.dissco.virtualcollectionservice.repository;

import static eu.dissco.virtualcollectionservice.database.jooq.Tables.VIRTUAL_COLLECTION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.virtualcollectionservice.exception.DisscoJsonBMappingException;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VirtualCollectionRepository {

  private final DSLContext context;
  private final ObjectMapper objectMapper;

  public Set<VirtualCollection> getAllVirtualCollections() {
    return new HashSet<>(context.select(VIRTUAL_COLLECTION.DATA).from(VIRTUAL_COLLECTION)
        .where(VIRTUAL_COLLECTION.TOMBSTONED.isNull())
        .fetchSet(jsonbRecord -> mapRecordToVirtualCollection(
            jsonbRecord.getValue(VIRTUAL_COLLECTION.DATA))));
  }

  private VirtualCollection mapRecordToVirtualCollection(JSONB value) {
    try {
      return objectMapper.readValue(value.data(), VirtualCollection.class);
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Unable to map virtual collection from jsonb", e);
    }
  }
}
