package org.example;

import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.io.BufferedWriter;
import java.io.IOException;

public class Entrypoint implements HttpFunction {
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        BufferedWriter writer = response.getWriter();
        String project_id = request.getFirstQueryParameter("projectid").orElse("cn2223-t1-g08");
        String zone = request.getFirstQueryParameter("zone").orElse("europe-west1-b");
        String grpName = request.getFirstQueryParameter("grpName").orElse("instance-group-servers");
        listManagedInstanceGroupVMs(project_id, zone, grpName, writer);
    }

    static void listManagedInstanceGroupVMs(String project, String zone, String grpName, BufferedWriter writer) throws IOException {
        try (InstancesClient client = InstancesClient.create()) {
            for (Instance instance : client.list(project, zone).iterateAll()) {
                if(instance.getName().contains(grpName) && instance.getStatus().equalsIgnoreCase("running")) {
                    String ip = instance.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                    writer.write(ip + ",");
                }
            }
        }
    }
}
