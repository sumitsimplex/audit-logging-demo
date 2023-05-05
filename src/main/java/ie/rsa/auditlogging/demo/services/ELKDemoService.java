package ie.rsa.auditlogging.demo.services;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ELKDemoService {

    // Initializing instance of Logger for Service
    private static final Logger log = LoggerFactory.getLogger(ELKDemoService.class);

    public JSONArray getAllFoodDetails(){
        log.info("Fetching ALL food details...");
        JSONArray foodDetail = new JSONArray();
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(FileUtils.readFileToString(ResourceUtils.getFile("classpath:example.json"), "UTF-8"));
            JSONObject jsonObject = (JSONObject) obj;
            foodDetail = (JSONArray) jsonObject.get("data");

        } catch (IOException | ParseException e) {
            log.error("Error occurred in reading JSON file");
            e.printStackTrace();
        }
        return foodDetail;
    }
}