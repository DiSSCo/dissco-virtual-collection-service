package eu.dissco.virtualcollectionservice.component;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import eu.dissco.virtualcollectionservice.repository.VirtualCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VirtualCollectionCacheComponentTest {

  @Mock
  private VirtualCollectionRepository repository;

  private VirtualCollectionCacheComponent cache;

  @BeforeEach
  void setUp() {
    cache = new VirtualCollectionCacheComponent(repository);
  }

  @Test
  void testFillCache() {
    // Given

    // When
    cache.fillCache();

    // Then
    then(repository).should().getAllVirtualCollections();
  }

  @Test
  void testRefreshCache() {
    // Given
    cache.fillCache();

    // When
    cache.refreshCache();

    // Then
    then(repository).should(times(2)).getAllVirtualCollections();
  }

}
