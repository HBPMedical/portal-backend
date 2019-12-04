/*
 * Developed by Kechagias Konstantinos.
 * Copyright (c) 2019. MIT License
 */

package eu.hbp.mip.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GenParameters {

    private static final Logger logger = LoggerFactory.getLogger(GenParameters.class);

    private static GenParameters genParams;

    private String jwtSecret;

    private String jwtIssuer;

    private String galaxyURL;

    private String galaxyApiKey;

    private String galaxyReverseProxyUsername;

    private String galaxyReverseProxyPassword;

    private GenParameters(){

    }

    public static GenParameters getGenParamInstance() {
        if (genParams == null) {
            logger.info("->>>>>>>Reading Enviroment variables");
            genParams = new GenParameters();

            genParams.setJwtSecret(System.getenv("JWT_SECRET"));
            genParams.setJwtIssuer(System.getenv("JWT_ISSUER"));
            genParams.setGalaxyURL(System.getenv("GALAXY_URL"));
            genParams.setGalaxyApiKey(System.getenv("GALAXY_API_KEY"));
            genParams.setGalaxyReverseProxyUsername(System.getenv("GALAXY_REVERSE_PROXY_USERNAME"));
            genParams.setGalaxyReverseProxyPassword(System.getenv("GALAXY_REVERSE_PROXY_PASSWORD"));


            //If environment variable not exists read from file.
            if (genParams.getJwtSecret() == null){
                throw new RuntimeException("Cannot find Enviroment Variables");
//                logger.info("->>>>>>>Reading from file");
//
//                File file = null;
//                try {
//                    file = new ClassPathResource("config.properties").getFile();
//                    InputStream input = new FileInputStream(file);
//                    Properties prop = new Properties();
//
//                    // load a properties file
//                    prop.load(input);
//
//                    // get the property value and print it out
//                    genParams.setJwtSecret(prop.getProperty("jwtSecret"));
//                    genParams.setJwtIssuer(prop.getProperty("jwtIssuer"));
//                    genParams.setGalaxyURL(prop.getProperty("galaxyURL"));
//                    genParams.setGalaxyApiKey(prop.getProperty("galaxyApiKey"));
//                    genParams.setGalaxyReverseProxyUsername(prop.getProperty("galaxyReverseProxyUsername"));
//                    genParams.setGalaxyReverseProxyPassword(prop.getProperty("galaxyReverseProxyPassword"));
//                } catch (IOException e) {
//                    logger.error("Cannot initialize GenParameters from config file", e);
//                }
            }
        }
        return genParams;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    private void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getJwtIssuer() {
        return  jwtIssuer;
    }

    private void setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    public String getGalaxyURL() {
        return galaxyURL;
    }

    private void setGalaxyURL(String galaxyURL) {
        this.galaxyURL = galaxyURL;
    }

    public String getGalaxyApiKey() {
        return galaxyApiKey;
    }

    private void setGalaxyApiKey(String galaxyApiKey) {
        this.galaxyApiKey = galaxyApiKey;
    }

    public String getGalaxyReverseProxyUsername() {
        return galaxyReverseProxyUsername;
    }

    public void setGalaxyReverseProxyUsername(String galaxyReverseProxyUsername) {
        this.galaxyReverseProxyUsername = galaxyReverseProxyUsername;
    }

    public String getGalaxyReverseProxyPassword() {
        return galaxyReverseProxyPassword;
    }

    public void setGalaxyReverseProxyPassword(String galaxyReverseProxyPassword) {
        this.galaxyReverseProxyPassword = galaxyReverseProxyPassword;
    }
}