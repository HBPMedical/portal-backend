/*
 * Created by mirco on 04.12.15.
 */

package eu.hbp.mip;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class MIPApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(MIPApplication.class);
	
    public static void main(String[] args) {
        SpringApplication.run(MIPApplication.class, args);
    }



	
}
