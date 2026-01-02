package com.ecommerce.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.dto.ProductDt;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;

@Controller
public class AdminController {

    @Autowired
    private CategoryService cservice;

    @Autowired
    private ProductService pservice;

    @Autowired
    private UserService userService;

    @Value("${upload.path}")
    private String uploadPath;

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/home";
    }

    @GetMapping("/admin/home")
    public String adminHome(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            model.addAttribute("adminEmail", email);

            Optional<User> adminOpt = userService.findByEmail(email);
            if (adminOpt.isPresent()) {
                model.addAttribute("adminUser", adminOpt.get());
            }
        }
        return "admin";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam("email") String email,
                           @RequestParam("password") String password) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(password);
        u.setRole("ROLE_USER");
        userService.save(u);
        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin/categories")
    public String categoryPage(Model model) {
        List<Category> list = cservice.getAll();
        model.addAttribute("categories", list);
        return "categories";
    }

    @GetMapping("/admin/categories/add")
    public String addCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "categoriesAdd";
    }

    @PostMapping("/admin/categories/add")
    public String postAddCategory(@ModelAttribute("category") Category c) {
        cservice.saveCategory(c);
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") int id) {
        cservice.deletebyId(id);
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/update/{id}")
    public String updateCategory(@PathVariable("id") int id, Model model) {
        Optional<Category> category = cservice.fetchbyId(id);
        if (category.isPresent()) {
            model.addAttribute("category", category.get());
            return "categoriesAdd";
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/products")
    public String productPage(Model model) {
        List<Product> list = pservice.getAll();
        model.addAttribute("products", list);
        return "products";
    }

    @GetMapping("/admin/products/add")
    public String addProductForm(Model model) {
        ProductDt p = new ProductDt();
        model.addAttribute("productDTO", p);
        model.addAttribute("categories", cservice.getAll());
        return "productsAdd";
    }

    @PostMapping("/admin/products/add")
public String postAddProduct(@ModelAttribute("productDTO") ProductDt p,
                             @RequestParam("productImage") MultipartFile file,
                             Model model) throws IOException {

    Product pro;
    boolean isUpdate = p.getId() != 0 && p.getId() > 0;

    if (isUpdate) {
        Optional<Product> existingOpt = pservice.fetchbyId(p.getId());
        if (existingOpt.isEmpty()) {
            return "redirect:/admin/products";
        }
        pro = existingOpt.get();
    } else {
        pro = new Product();
    }

    pro.setName(p.getName());
    pro.setPrice(p.getPrice());
    pro.setWeight(p.getWeight());
    pro.setDescription(p.getDescription());

    Optional<Category> categoryOpt = cservice.fetchbyId(p.getCategoryId());
    if (categoryOpt.isEmpty()) {
        model.addAttribute("error", "Please select a valid category.");
        model.addAttribute("categories", cservice.getAll());
        model.addAttribute("productDTO", p);
        return "productsAdd";
    }
    pro.setCategory(categoryOpt.get());

    String imageName = pro.getImageName(); // Keep existing image name by default

    if (!file.isEmpty()) {
        // Create unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        imageName = System.currentTimeMillis() + "_" + (int)(Math.random() * 10000) + extension;

        // Ensure directory exists
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Save new image
        Path filePath = Paths.get(uploadPath, imageName);
        Files.write(filePath, file.getBytes());

        // Delete old image if updating and filename changed
        if (isUpdate && pro.getImageName() != null && !pro.getImageName().equals(imageName)) {
            Path oldPath = Paths.get(uploadPath, pro.getImageName());
            try {
                Files.deleteIfExists(oldPath);
            } catch (Exception ignored) {}
        }
    }

    pro.setImageName(imageName);
    pservice.saveProduct(pro);

    return "redirect:/admin/products";
}

    @GetMapping("/admin/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") long id) {
        pservice.deletebyId(id);
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/products/update/{id}")
    public String updateProduct(@PathVariable("id") long id, Model model) {
        Optional<Product> proOpt = pservice.fetchbyId(id);
        if (proOpt.isPresent()) {
            Product pro = proOpt.get();
            ProductDt pdt = new ProductDt();
            pdt.setId(pro.getId());
            pdt.setName(pro.getName());
            pdt.setPrice(pro.getPrice());
            pdt.setWeight(pro.getWeight());
            pdt.setDescription(pro.getDescription());
            pdt.setCategoryId(pro.getCategory().getId());
            pdt.setImageName(pro.getImageName());

            model.addAttribute("productDTO", pdt);
            model.addAttribute("categories", cservice.getAll());
            return "productsAdd";
        }
        return "redirect:/admin/products";
    }
}