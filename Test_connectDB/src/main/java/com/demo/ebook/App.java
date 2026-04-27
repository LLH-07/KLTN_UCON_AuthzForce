package com.demo.ebook;

import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        String pdpConf = "file:///D:/Work/Study/Learning/KLTN/Ucon_ABC/test/02/src/main/resources/pdp.xml";
        String catalog = "file:///D:/Work/Study/Learning/KLTN/Ucon_ABC/test/02/src/main/resources/xml-catalog.xml";
        String extXsd = "file:///D:/Work/Study/Learning/KLTN/Ucon_ABC/test/02/src/main/resources/pdp-ext.xsd";

        PolicyEngineManager manager = new PolicyEngineManager(pdpConf, catalog, extXsd);
        manager.init();
        // PDP is loaded once at startup; auto reload only rebuilds it when policy metadata changes.
        manager.startAutoReload(10); // mỗi 10 giây kiểm tra policy có đổi không

        AuthzforceEvaluator evaluator = new AuthzforceEvaluator(manager);

        Runtime.getRuntime().addShutdownHook(new Thread(manager::shutdown));

        System.out.println("=== APP STARTED ===");
        System.out.println("Commands:");
        System.out.println("  eval <sessionId> <requestedChunk>");
        System.out.println("  reload");
        System.out.println("  status");
        System.out.println("  quit");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();

                if (line.equalsIgnoreCase("quit")) {
                    break;
                }

                if (line.equalsIgnoreCase("reload")) {
                    manager.manualReload();
                    continue;
                }

                if (line.equalsIgnoreCase("status")) {
                    System.out.println("CURRENT FINGERPRINT => " + manager.getCurrentFingerprint());
                    continue;
                }

                if (line.startsWith("eval ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length != 3) {
                        System.out.println("Sai cú pháp. Dùng: eval <sessionId> <requestedChunk>");
                        continue;
                    }

                    int sessionId = Integer.parseInt(parts[1]);
                    int requestedChunk = Integer.parseInt(parts[2]);

                    String decision = evaluator.evaluate(sessionId, requestedChunk);
                    System.out.println("DECISION => " + decision);
                    continue;
                }

                System.out.println("Lệnh không hợp lệ.");
            }
        }

        manager.shutdown();
        System.out.println("=== APP STOPPED ===");
    }
}
