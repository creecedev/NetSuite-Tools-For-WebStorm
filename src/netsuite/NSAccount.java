package netsuite;

public class NSAccount implements Comparable<NSAccount> {
    private String accountId;
    private String accountName;
    private String accountEmail;
    private String accountPassword;
    private String roleId;
    private String roleName;
    private String restDomain;
    private String productionWebServicesDomain;
    private String sandboxWebServicesDomain;
    private String systemDomain;

    public NSAccount(String accountId, String accountName, String accountEmail, String accountPassword, String roleId, String roleName, String restDomain, String productionWebServicesDomain, String sandboxWebServicesDomain, String systemDomain) {
        this.accountId                       = accountId;
        this.accountName                     = accountName;
        this.accountEmail                    = accountEmail;
        this.accountPassword                 = accountPassword;
        this.roleId                          = roleId;
        this.roleName                        = roleName;
        this.restDomain                      = restDomain;
        this.productionWebServicesDomain     = productionWebServicesDomain;
        this.sandboxWebServicesDomain        = sandboxWebServicesDomain;
        this.systemDomain                    = systemDomain;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getRestDomain() {
        return restDomain;
    }

    public String getProductionWebServicesDomain() {
        return productionWebServicesDomain;
    }

    public String getSandboxWebServicesDomain() {
        return sandboxWebServicesDomain;
    }

    public String getSystemDomain() {
        return systemDomain;
    }

    public int compareTo(NSAccount nsAccount) {
        return  this.getAccountName().compareTo(nsAccount.getAccountName());
    }
}
