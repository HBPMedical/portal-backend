/*
 * Developed by Kechagias Konstantinos.
 * Copyright (c) 2019. MIT License
 */

package eu.hbp.mip.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenParameters {

    private static final Logger logger = LoggerFactory.getLogger(GenParameters.class);

    private static GenParameters genParams;

    private String jwtSecret;

    private String jwtIssuer;

    private String galaxyURL;

    private String galaxyApiKey;

    private String galaxyReverseProxyUsername;

    private String galaxyReverseProxyPassword;

    private GenParameters() {

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

            if (genParams.getJwtSecret() == null) {
                throw new RuntimeException("Cannot find Environment Variables");
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
        return jwtIssuer;
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