package com.forgeStackk.EduResolve.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to fetch or generate image URLs for educational content.
 * Uses Wikipedia and other free educational image sources.
 */
@Service
public class ImageService {

    /**
     * Generates educational image URLs based on the answer text.
     * Extracts key nouns and searches for relevant images.
     *
     * @param question The student's original question
     * @param answer The AI-generated or textbook answer
     * @return A list of image URLs related to the content
     */
    public List<String> generateImageUrls(String question, String answer) {
        List<String> images = new ArrayList<>();
        
        // Extract key terms from question and answer
        List<String> keyTerms = extractKeyTerms(question, answer);
        
        // Generate Wikipedia search image URLs for key terms
        for (String term : keyTerms) {
            String imageUrl = generateWikipediaImageUrl(term);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                images.add(imageUrl);
            }
            // Limit to 2-3 images per response
            if (images.size() >= 3) break;
        }
        
        return images;
    }

    /**
     * Extracts key educational terms from question and answer.
     */
    private List<String> extractKeyTerms(String question, String answer) {
        List<String> terms = new ArrayList<>();
        
        // Extract capitalized words (likely nouns) from both strings
        Pattern pattern = Pattern.compile("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b");
        
        String combined = question + " " + answer;
        Matcher matcher = pattern.matcher(combined);
        
        while (matcher.find() && terms.size() < 5) {
            String term = matcher.group().trim();
            if (term.length() > 3 && !isCommonWord(term)) {
                terms.add(term);
            }
        }
        
        return terms;
    }

    /**
     * Generates a Wikipedia image search URL for a given term.
     * Uses Wikimedia Commons API.
     */
    private String generateWikipediaImageUrl(String searchTerm) {
        // URL-encode the search term
        String encoded = searchTerm.replaceAll("\\s+", "+");
        
        // Return a URL to search for images in Wikimedia Commons
        // This can be replaced with actual API calls later
        return String.format(
            "https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=%s&srprop=snippet&format=json&srnamespace=6",
            encoded
        );
    }

    /**
     * Checks if a word is too common to be useful.
     */
    private boolean isCommonWord(String term) {
        String[] commonWords = {
            "The", "This", "That", "What", "When", "Where", "Why", 
            "How", "Which", "Would", "Could", "Should", "Please"
        };
        
        for (String common : commonWords) {
            if (term.equalsIgnoreCase(common)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Gets a direct educational image URL for common topics.
     * This is a fallback mechanism with known educational resources.
     */
    public List<String> getDirectImageUrls(String topic) {
        List<String> urls = new ArrayList<>();
        
        // Map of common topics to educational image URLs
        String lowerTopic = topic.toLowerCase();
        
        if (lowerTopic.contains("photosynthesis")) {
            urls.add("https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/Photosynthesis.svg/1200px-Photosynthesis.svg.png");
        }
        if (lowerTopic.contains("mitochondria") || lowerTopic.contains("cell")) {
            urls.add("https://upload.wikimedia.org/wikipedia/commons/thumb/b/ba/Animal_cell_structure_en.svg/1024px-Animal_cell_structure_en.svg.png");
        }
        if (lowerTopic.contains("dna") || lowerTopic.contains("chromosome")) {
            urls.add("https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/DNA_Structure_and_Nomenclature.svg/1024px-DNA_Structure_and_Nomenclature.svg.png");
        }
        if (lowerTopic.contains("atom") || lowerTopic.contains("molecule")) {
            urls.add("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Diagram_of_an_atom.svg/800px-Diagram_of_an_atom.svg.png");
        }
        if (lowerTopic.contains("periodic table") || lowerTopic.contains("element")) {
            urls.add("https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Periodic_table.svg/1000px-Periodic_table.svg.png");
        }
        if (lowerTopic.contains("photosynthesis") || lowerTopic.contains("chloroplast")) {
            urls.add("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Chloroplast_structure.svg/1024px-Chloroplast_structure.svg.png");
        }
        
        return urls;
    }
}
