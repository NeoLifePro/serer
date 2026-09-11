package steam.vm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/steam")
public class SteamItemController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/item-image")
    public ResponseEntity<?> getItemImage(@RequestParam String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);

            String url = "https://steamcommunity.com/market/search/render/?" +
                    "query=" + encodedName +
                    "&start=0" +
                    "&count=10" +
                    "&search_descriptions=0" +
                    "&sort_column=name" +
                    "&sort_dir=asc" +
                    "&appid=730" +
                    "&norender=1";

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                return ResponseEntity.status(response.statusCode())
                        .body(new ItemImageResponse(null));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.get("results");

            if (results == null || !results.isArray() || results.isEmpty()) {
                return ResponseEntity.ok(new ItemImageResponse(null));
            }

            JsonNode selectedItem = null;

            for (JsonNode item : results) {
                String hashName = item.path("hash_name").asText("");
                String itemName = item.path("name").asText("");

                if (hashName.equalsIgnoreCase(name) || itemName.equalsIgnoreCase(name)) {
                    selectedItem = item;
                    break;
                }
            }

            if (selectedItem == null) {
                selectedItem = results.get(0);
            }

            JsonNode assetDescription = selectedItem.get("asset_description");

            if (assetDescription == null) {
                return ResponseEntity.ok(new ItemImageResponse(null));
            }

            String iconUrlLarge = assetDescription.path("icon_url_large").asText("");
            String iconUrl = assetDescription.path("icon_url").asText("");

            String icon = !iconUrlLarge.isBlank() ? iconUrlLarge : iconUrl;

            if (icon.isBlank()) {
                return ResponseEntity.ok(new ItemImageResponse(null));
            }

            String fullImageUrl = "https://community.cloudflare.steamstatic.com/economy/image/" + icon;

            return ResponseEntity.ok(new ItemImageResponse(fullImageUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ItemImageResponse(null));
        }
    }

    public record ItemImageResponse(String imageUrl) {
    }
}