package com.github.jacopofar.fleximatcherwebinterface.annotators;

import com.github.jacopofar.fleximatcher.rule.RuleFactory;
import com.github.jacopofar.fleximatcher.rules.MatchingRule;
import com.github.jacopofar.fleximatcherwebinterface.exceptions.RuntimeJSONCarryingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created on 2016-06-29.
 */
public class HTTPRuleFactory implements RuleFactory {

    private final URL url;
    private URL samplerUrl;

    public HTTPRuleFactory(String annotatorUrl, String samplerUrl) throws MalformedURLException {
        this.url = new URL(annotatorUrl);
        if(samplerUrl != null)
            this.samplerUrl = new URL(samplerUrl);
    }

    @Override
    public MatchingRule getRule(String parameter) {
        return new HTTPRule(url, parameter);
    }

    @Override
    public String generateSample(String parameter) {
        if (this.samplerUrl == null)
            return null;
        try {
            HttpResponse<String> response = Unirest.post(samplerUrl.toString())
                    .header("content-type", "application/json")
                    .body("{\"parameter\":" + JSONObject.quote(parameter) +  "}")
                    .asString();
            if(response.getStatus() != 200){
                JSONObject errObj = new JSONObject();
                try {
                    errObj.put("http_status",response.getStatus());
                    errObj.put("endpoint",samplerUrl.toString());
                    errObj.put("error_body",response.getBody());
                } catch (JSONException e) {
                    //can never happen...
                    e.printStackTrace();
                }
                throw new RuntimeJSONCarryingException("Error from the external annotator", errObj);


            }
            //The empty string is the way an HTTP annotator tells it failed to generate an utterance. Use null
            return response.getBody().isEmpty() ? null : response.getBody();
        } catch (UnirestException e) {
            e.printStackTrace();
            throw new RuntimeException("error generating sample from external annotator",e);
        }
    }
}
