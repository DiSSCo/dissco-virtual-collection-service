package eu.dissco.virtualcollectionservice.component;

import eu.dissco.virtualcollectionservice.repository.VirtualCollectionRepository;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VirtualCollectionCacheComponent {

  private final VirtualCollectionRepository repository;
  @Getter
  private Set<VirtualCollection> cache;

  @PostConstruct
  public void fillCache() {
    cache = repository.getAllVirtualCollections();
    log.info("Virtual Collection Cache Initialized, total Virtual Collections: {}", cache.size());
  }

  @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS) // Refresh every hour
  public void refreshCache() {
    log.info("Refreshing Virtual Collection Cache");
    cache.clear();
    cache.addAll(repository.getAllVirtualCollections());
    log.info("Virtual Collection Cache refreshed, total Virtual Collections: {}", cache.size());
  }

}
