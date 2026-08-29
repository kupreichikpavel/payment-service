package by.innowise.paymentservice.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoLiquibaseConfig {

  private static final String CHANGELOG =
      "db/changelog/db.changelog-master.yaml";

  @Bean
  public ApplicationRunner mongoLiquibaseRunner(
      @Value("${spring.mongodb.uri}") String mongoUri
  ) {
    return args -> {
      ClassLoaderResourceAccessor resourceAccessor =
          new ClassLoaderResourceAccessor();

      Database database = DatabaseFactory.getInstance()
          .openDatabase(
              mongoUri,
              null,
              null,
              null,
              resourceAccessor
          );

      try {
        Liquibase liquibase = new Liquibase(
            CHANGELOG,
            resourceAccessor,
            database
        );

        liquibase.update("");
      } finally {
        database.close();
      }
    };
  }
}