package com.scalar.db.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.scalar.db.api.*;
import com.scalar.db.common.FilterableScanner;
import com.scalar.db.common.checker.OperationChecker;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class S3Test {
  private static final int ANY_LIMIT = 100;

  private S3 s3;
  @Mock private S3ClientWrapper s3ClientWrapper;
  @Mock private SelectStatementHandler selectStatementHandler;
  @Mock private PutStatementHandler putStatementHandler;
  @Mock private DeleteStatementHandler deleteStatementHandler;
  @Mock private BatchHandler batchHandler;
  @Mock private OperationChecker operationChecker;
  @Mock private Scanner scanner;
  @Mock private Key partitionKey;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    Properties s3ConfigProperties = new Properties();
    s3 =
        new S3(
            new DatabaseConfig(s3ConfigProperties),
            s3ClientWrapper,
            selectStatementHandler,
            putStatementHandler,
            deleteStatementHandler,
            batchHandler,
            operationChecker);
  }

  @Test
  public void get_WithoutConjunction_ShouldHandledWithOriginalGet() throws ExecutionException {
    // Arrange
    Get get =
        Get.newBuilder()
            .namespace("ns")
            .table("tbl")
            .partitionKey(partitionKey)
            .projection("col1")
            .build();
    when(selectStatementHandler.handle(any(Get.class))).thenReturn(scanner);

    // Act
    Optional<Result> actual = s3.get(get);

    // Assert
    assertThat(actual.isPresent()).isFalse();
    ArgumentCaptor<Get> captor = ArgumentCaptor.forClass(Get.class);
    verify(selectStatementHandler).handle(captor.capture());
    Get actualGet = captor.getValue();
    assertThat(actualGet).isEqualTo(get);
  }

  @Test
  public void get_WithConjunctionWithoutProjections_ShouldHandledWithoutProjections()
      throws ExecutionException {
    // Arrange
    Get get =
        Get.newBuilder()
            .namespace("ns")
            .table("tbl")
            .partitionKey(partitionKey)
            .where(ConditionBuilder.column("col2").isLessThanInt(0))
            .build();
    when(selectStatementHandler.handle(any(Get.class))).thenReturn(scanner);

    // Act
    Optional<Result> actual = s3.get(get);

    // Assert
    assertThat(actual.isPresent()).isFalse();
    ArgumentCaptor<Get> captor = ArgumentCaptor.forClass(Get.class);
    verify(selectStatementHandler).handle(captor.capture());
    Get actualGet = captor.getValue();
    assertThat(actualGet.getProjections()).isEmpty();
  }

  @Test
  public void get_WithConjunctionAndProjections_ShouldHandledWithExtendedProjections()
      throws ExecutionException {
    // Arrange
    Get get =
        Get.newBuilder()
            .namespace("ns")
            .table("tbl")
            .partitionKey(partitionKey)
            .projections("col1")
            .where(ConditionBuilder.column("col2").isLessThanInt(0))
            .build();
    when(selectStatementHandler.handle(any(Get.class))).thenReturn(scanner);

    // Act
    Optional<Result> actual = s3.get(get);

    // Assert
    assertThat(actual.isPresent()).isFalse();
    ArgumentCaptor<Get> captor = ArgumentCaptor.forClass(Get.class);
    verify(selectStatementHandler).handle(captor.capture());
    Get actualGet = captor.getValue();
    assertThat(actualGet.getProjections()).containsExactlyInAnyOrder("col1", "col2");
  }

  @Test
  public void scan_WithLimitWithoutConjunction_ShouldHandledWithLimit() throws ExecutionException {
    // Arrange
    Scan scan = Scan.newBuilder().namespace("ns").table("tbl").all().limit(ANY_LIMIT).build();
    when(selectStatementHandler.handle(scan)).thenReturn(scanner);

    // Act
    Scanner actual = s3.scan(scan);

    // Assert
    assertThat(actual).isInstanceOf(Scanner.class);
    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(selectStatementHandler).handle(captor.capture());
    Scan actualScan = captor.getValue();
    assertThat(actualScan.getLimit()).isEqualTo(ANY_LIMIT);
  }

  @Test
  public void scan_WithLimitAndConjunction_ShouldHandledWithoutLimit() throws ExecutionException {
    // Arrange
    Scan scan =
        Scan.newBuilder()
            .namespace("ns")
            .table("tbl")
            .all()
            .where(mock(ConditionalExpression.class))
            .limit(ANY_LIMIT)
            .build();
    when(selectStatementHandler.handle(scan)).thenReturn(scanner);

    // Act
    Scanner actual = s3.scan(scan);

    // Assert
    assertThat(actual).isInstanceOf(FilterableScanner.class);
    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(selectStatementHandler).handle(captor.capture());
    Scan actualScan = captor.getValue();
    assertThat(actualScan.getLimit()).isEqualTo(0);
  }

  @Test
  public void scan_WithConjunctionWithoutProjections_ShouldHandledWithoutProjections()
      throws ExecutionException {
    // Arrange
    Scan scan =
        Scan.newBuilder()
            .namespace("ns")
            .table("tbl")
            .all()
            .where(ConditionBuilder.column("col2").isLessThanInt(0))
            .build();
    when(selectStatementHandler.handle(scan)).thenReturn(scanner);

    // Act
    Scanner actual = s3.scan(scan);

    // Assert
    assertThat(actual).isInstanceOf(FilterableScanner.class);
    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(selectStatementHandler).handle(captor.capture());
    Scan actualScan = captor.getValue();
    assertThat(actualScan.getProjections()).isEmpty();
  }

  @Test
  public void scan_WithConjunctionAndProjections_ShouldHandledWithExtendedProjections()
      throws ExecutionException {
    // Arrange
    Scan scan =
        Scan.newBuilder()
            .namespace("ns")
            .table("tbl")
            .all()
            .projections("col1")
            .where(ConditionBuilder.column("col2").isLessThanInt(0))
            .build();
    when(selectStatementHandler.handle(scan)).thenReturn(scanner);

    // Act
    Scanner actual = s3.scan(scan);

    // Assert
    assertThat(actual).isInstanceOf(FilterableScanner.class);
    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(selectStatementHandler).handle(captor.capture());
    Scan actualScan = captor.getValue();
    assertThat(actualScan.getProjections()).containsExactlyInAnyOrder("col1", "col2");
  }
}
