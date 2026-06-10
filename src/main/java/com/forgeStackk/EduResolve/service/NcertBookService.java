package com.forgeStackk.EduResolve.service;

import com.forgeStackk.EduResolve.entity.NcertBook;
import com.forgeStackk.EduResolve.repository.NcertBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing NCERT Book operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NcertBookService {

    private final NcertBookRepository ncertBookRepository;
    private final GitHubApiService gitHubApiService;

    /**
     * Get all books
     */
    public List<NcertBook> getAllBooks() {
        return ncertBookRepository.findAll();
    }

    /**
     * Get book by ID
     */
    public NcertBook getBookById(Long id) {
        Optional<NcertBook> book = ncertBookRepository.findById(id);
        return book.orElse(null);
    }

    /**
     * Get books by class and subject
     */
    public List<NcertBook> getBooksByClassAndSubject(String classGrade, String subject) {
        return ncertBookRepository.findByClassGradeAndSubject(classGrade, subject);
    }

    /**
     * Get books by subject
     */
    public List<NcertBook> getBooksBySubject(String subject) {
        return ncertBookRepository.findBySubject(subject);
    }

    /**
     * Get books by class
     */
    public List<NcertBook> getBooksByClass(String classGrade) {
        return ncertBookRepository.findByClassGrade(classGrade);
    }

    /**
     * Save a book
     */
    public NcertBook saveBook(NcertBook book) {
        return ncertBookRepository.save(book);
    }

    /**
     * Sync books from GitHub repository
     */
    public void syncBooksFromGitHub() {
        try {
            // Get available classes from GitHub
            List<String> classes = gitHubApiService.getAvailableClasses();
            
            for (String className : classes) {
                List<String> subjects = gitHubApiService.getSubjectsByClass(className);
                
                for (String subject : subjects) {
                    List<GitHubApiService.GitHubBookInfo> githubBooks = 
                        gitHubApiService.getBooks(className, subject);
                    
                    for (GitHubApiService.GitHubBookInfo bookInfo : githubBooks) {
                        // Check if book already exists
                        List<NcertBook> existingBooks = ncertBookRepository
                            .findByClassGradeAndSubject(className, subject);
                        
                        boolean bookExists = existingBooks.stream()
                            .anyMatch(book -> book.getPdfFilename().equals(bookInfo.getFilename()));
                        
                        if (!bookExists) {
                            // Create new book entry
                            NcertBook book = new NcertBook();
                            book.setClassGrade(className);
                            book.setSubject(subject);
                            book.setTitle(bookInfo.getTitle());
                            book.setPdfFilename(bookInfo.getFilename());
                            
                            ncertBookRepository.save(book);
                            log.info("Saved new book: {} for {} {}", book.getTitle(), className, subject);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error syncing books from NCERT resources", e);
        }
    }

    /**
     * Delete a book
     */
    public void deleteBook(Long id) {
        ncertBookRepository.deleteById(id);
    }

    /**
     * Update book information
     */
    public NcertBook updateBook(Long id, NcertBook bookDetails) {
        Optional<NcertBook> optionalBook = ncertBookRepository.findById(id);
        if (optionalBook.isPresent()) {
            NcertBook book = optionalBook.get();
            book.setClassGrade(bookDetails.getClassGrade());
            book.setSubject(bookDetails.getSubject());
            book.setTitle(bookDetails.getTitle());
            book.setGithubUrl(bookDetails.getGithubUrl());
            book.setGithubRepo(bookDetails.getGithubRepo());
            book.setGithubPath(bookDetails.getGithubPath());
            book.setPdfFilename(bookDetails.getPdfFilename());
            book.setTotalPages(bookDetails.getTotalPages());
            return ncertBookRepository.save(book);
        }
        return null;
    }
}
