package netsuite;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class NSRolesRestServiceController {
    final private String NS_WSDL_VERSION                           = "2016_1";
    final private String NS_ENVIRONMENT_PRODUCTION                 = "Production";
    final private String NS_ENVIRONMENT_SANDBOX                    = "Sandbox";
    final private String NS_ENVIRONMENT_RELEASE_PREVIEW            = "Release Preview";
    final private String NS_ROLES_REST_SERVICE_URL_PRODUCTION      = "https://rest.netsuite.com/rest/roles";
    final private String NS_ROLES_REST_SERVICE_URL_SANDBOX         = "https://rest.sandbox.netsuite.com/rest/roles";
    final private String NS_ROLES_REST_SERVICE_URL_RELEASE_PREVIEW = "https://rest.na1.beta.netsuite.com/rest/roles";
    final private String NS_WEB_SERVICES_END_POINT                 = "/services/NetSuitePort_" + NS_WSDL_VERSION;
    final private String NS_SANDBOX_WEB_SERVICES_URL               = "https://webservices.sandbox.netsuite.com";
    final private String NS_RELEASE_PREVIEW_WEB_SERVICES_URL       = "https://webservices.na1.beta.netsuite.com";
    private final String ADMINISTRATOR_ROLE_ID                     = "3";
    private final String FULL_ACCESS_ROLE_ID                       = "18";

    private String buildNLAuthString(String nsEmail, String nsPassword) {
        return "NLAuth nlauth_email=" + nsEmail + ", nlauth_signature=" + nsPassword;
    }

    private String getNSRolesRestServiceJSON(String nsEmail, String nsPassword, String nsEnvironment) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(getEnvironmentRolesRestServiceURL(nsEnvironment)).openConnection();
            connection.setAllowUserInteraction(Boolean.FALSE);
            connection.setInstanceFollowRedirects(Boolean.FALSE);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", buildNLAuthString(nsEmail, nsPassword));
            return new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
        } catch (Exception ex) {
            return null;
        }
    }

    private String getEnvironmentRolesRestServiceURL(String nsEnvironment) {
        String rolesRestServiceURL = null;

        if (nsEnvironment.equals(NS_ENVIRONMENT_PRODUCTION)) {
            rolesRestServiceURL = NS_ROLES_REST_SERVICE_URL_PRODUCTION;
        } else if (nsEnvironment.equals(NS_ENVIRONMENT_SANDBOX)) {
            rolesRestServiceURL = NS_ROLES_REST_SERVICE_URL_SANDBOX;
        } else if (nsEnvironment.equals(NS_ENVIRONMENT_RELEASE_PREVIEW)) {
            rolesRestServiceURL = NS_ROLES_REST_SERVICE_URL_RELEASE_PREVIEW;
        }

        return rolesRestServiceURL;
    }

    private ArrayList<NSAccount> getNSAccountsList(String nsRolesRestServiceJSON, String nsEmail, String nsPassword) {
        ArrayList<NSAccount> nsAccounts = null;

        if (nsRolesRestServiceJSON != null) {

            if (nsRolesRestServiceJSON.length() > 0) {
                try {
                    nsAccounts = new ArrayList<NSAccount>();

                    JSONArray accountsJSON = new JSONArray(nsRolesRestServiceJSON);

                    for (int i = 0; i < accountsJSON.length(); i++) {
                        JSONObject accountJSON = accountsJSON.getJSONObject(i);

                        if (accountJSON.has("account") && accountJSON.has("dataCenterURLs")) {
                            if (accountJSON.getJSONObject("role").get("internalId").toString().equals(ADMINISTRATOR_ROLE_ID) ||
                                    accountJSON.getJSONObject("role").get("internalId").toString().equals(FULL_ACCESS_ROLE_ID)) {
                                        nsAccounts.add(new NSAccount(accountJSON.getJSONObject("account").get("internalId").toString(),
                                        accountJSON.getJSONObject("account").get("name").toString(),
                                        nsEmail,
                                        nsPassword,
                                        accountJSON.getJSONObject("role").get("internalId").toString(),
                                        accountJSON.getJSONObject("role").get("name").toString(),
                                        accountJSON.getJSONObject("dataCenterURLs").get("restDomain").toString(),
                                        accountJSON.getJSONObject("dataCenterURLs").get("webservicesDomain").toString().concat(NS_WEB_SERVICES_END_POINT),
                                        NS_SANDBOX_WEB_SERVICES_URL.concat(NS_WEB_SERVICES_END_POINT),
                                        NS_RELEASE_PREVIEW_WEB_SERVICES_URL.concat(NS_WEB_SERVICES_END_POINT),
                                        accountJSON.getJSONObject("dataCenterURLs").get("systemDomain").toString()));
                            }
                        }
                    }
                } catch (Exception ex) {
                    return null;
                }
            }
        }

        return nsAccounts;
    }

    public ArrayList<NSAccount> getNSAccounts(String nsEmail, String nsPassword, String nsEnvironment) {
        if (nsEmail != null && !nsEmail.isEmpty() && nsPassword != null && !nsPassword.isEmpty() && nsEnvironment != null && !nsEnvironment.isEmpty()) {
            return getNSAccountsList(getNSRolesRestServiceJSON(nsEmail, nsPassword, nsEnvironment), nsEmail, nsPassword);
        }

        return null;
    }
}