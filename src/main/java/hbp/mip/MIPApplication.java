/*
 * Created by mirco on 04.12.15.
 */

package hbp.mip;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;

@SpringBootApplication(exclude = RepositoryRestMvcAutoConfiguration.class)
public class MIPApplication {

    public static void main(String[] args) {
        SpringApplication.run(MIPApplication.class, args);
    }

}
