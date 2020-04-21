package eu.hbp.mip.utils;

public class ClaimUtils {
    public static String allDatasetsAllowedClaim(){
        return "dataset_all";
    }

    public static String getDatasetClaim(String datasetCode){
        return "dataset_" + datasetCode;
    }
}
