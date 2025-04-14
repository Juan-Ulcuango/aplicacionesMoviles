package ec.edu.utn.example.vlsmapp02;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    EditText ipInput, maskInput, subnetCountInput;
    LinearLayout hostsInputContainer;
    Button generateButton, calculateButton;
    TableLayout resultsTable;
    ArrayList<EditText> hostInputs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // ¡Muy importante!

        ipInput = findViewById(R.id.ipInput);
        maskInput = findViewById(R.id.maskInput);
        subnetCountInput = findViewById(R.id.subnetCountInput);
        hostsInputContainer = findViewById(R.id.hostsInputContainer);
        generateButton = findViewById(R.id.generateSubnetsButton);
        calculateButton = findViewById(R.id.calculateButton);
        resultsTable = findViewById(R.id.resultsTable);

        generateButton.setOnClickListener(v -> generateHostInputs());
        calculateButton.setOnClickListener(v -> calculateVLSM());
    }

    private void generateHostInputs() {
        hostsInputContainer.removeAllViews();
        hostInputs.clear();

        String countText = subnetCountInput.getText().toString().trim();
        if (countText.isEmpty()) {
            Toast.makeText(this, "Ingresa la cantidad de subredes", Toast.LENGTH_SHORT).show();
            return;
        }

        int subnetCount = Integer.parseInt(countText);

        for (int i = 0; i < subnetCount; i++) {
            EditText input = new EditText(this);
            input.setHint("Hosts para Subred " + (i + 1));
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            hostsInputContainer.addView(input);
            hostInputs.add(input);
        }
    }

    private void calculateVLSM() {
        resultsTable.removeAllViews();

        String ipText = ipInput.getText().toString().trim();
        String maskText = maskInput.getText().toString().trim();

        if (ipText.isEmpty() || maskText.isEmpty()) {
            Toast.makeText(this, "Ingresa IP y máscara", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] ipParts = ipText.split("\\.");
        if (ipParts.length != 4) {
            Toast.makeText(this, "IP inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] baseIp;
        try {
            baseIp = Arrays.stream(ipParts).mapToInt(Integer::parseInt).toArray();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Formato de IP incorrecto", Toast.LENGTH_SHORT).show();
            return;
        }

        int cidr;
        try {
            cidr = Integer.parseInt(maskText);
            if (cidr < 1 || cidr > 32) {
                Toast.makeText(this, "Máscara CIDR inválida", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Máscara inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> hostsList = new ArrayList<>();
        for (EditText et : hostInputs) {
            String text = et.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos de hosts", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int val = Integer.parseInt(text);
                if (val <= 0) {
                    Toast.makeText(this, "El número de hosts debe ser mayor que 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                hostsList.add(val);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Valor inválido para hosts", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        List<Subnet> subnets = calculateSubnets(baseIp, cidr, hostsList);
        showResults(subnets);
    }

    private List<Subnet> calculateSubnets(int[] baseIp, int cidr, List<Integer> hostsPerSubnet) {
        List<Subnet> result = new ArrayList<>();
        hostsPerSubnet.sort(Collections.reverseOrder());

        long baseAddress = ipToLong(baseIp);
        for (int requiredHosts : hostsPerSubnet) {
            int neededBits = (int) Math.ceil(Math.log(requiredHosts + 2) / Math.log(2));
            int subnetMask = 32 - neededBits;
            long subnetSize = (long) Math.pow(2, neededBits);
            long networkAddress = baseAddress;
            long broadcastAddress = networkAddress + subnetSize - 1;

            result.add(new Subnet(subnetMask, networkAddress, broadcastAddress));
            baseAddress = broadcastAddress + 1;
        }

        return result;
    }

    private void showResults(List<Subnet> subnets) {
        TableRow header = new TableRow(this);
        header.addView(makeTextView("Subred"));
        header.addView(makeTextView("IP Red"));
        header.addView(makeTextView("Rango de Hosts"));
        header.addView(makeTextView("Broadcast"));
        resultsTable.addView(header);

        for (int i = 0; i < subnets.size(); i++) {
            Subnet s = subnets.get(i);
            TableRow row = new TableRow(this);
            row.addView(makeTextView("Subred " + (i + 1)));
            row.addView(makeTextView(longToIp(s.network) + "/" + s.cidr));
            row.addView(makeTextView(longToIp(s.network + 1) + " - " + longToIp(s.broadcast - 1)));
            row.addView(makeTextView(longToIp(s.broadcast)));
            resultsTable.addView(row);
        }
    }

    private TextView makeTextView(String text) {
        TextView tv = new TextView(this);
        tv.setPadding(8, 8, 8, 8);
        tv.setText(text);
        return tv;
    }

    private long ipToLong(int[] ip) {
        return ((long) ip[0] << 24) | ((long) ip[1] << 16) | ((long) ip[2] << 8) | ip[3];
    }

    private String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                (ip & 0xFF);
    }

    static class Subnet {
        int cidr;
        long network;
        long broadcast;

        Subnet(int cidr, long network, long broadcast) {
            this.cidr = cidr;
            this.network = network;
            this.broadcast = broadcast;
        }
    }
}
