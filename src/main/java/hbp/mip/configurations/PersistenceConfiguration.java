package hbp.mip.configurations;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = {"hbp.mip.experiment", "hbp.mip.user"})
public class PersistenceConfiguration {

    @Primary
    @Bean(name = "datasource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource portalDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "entityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
        emfb.setDataSource(portalDataSource());
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emfb.setJpaVendorAdapter(vendorAdapter);
        emfb.setPackagesToScan("hbp.mip.experiment", "hbp.mip.user");

        return emfb;
    }

    @Bean(name = "flyway")
    public Flyway flyway() {
        return Flyway.configure()  // ✅ Correct way to configure Flyway
                .dataSource(portalDataSource())  // ✅ Assign the DataSource properly
                .baselineOnMigrate(true)  // ✅ Use configuration method instead of setBaselineOnMigrate()
                .load();  // ✅ Create Flyway instance
    }
}
