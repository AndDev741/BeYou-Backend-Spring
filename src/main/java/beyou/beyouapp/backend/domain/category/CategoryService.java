package beyou.beyouapp.backend.domain.category;

import beyou.beyouapp.backend.domain.category.dto.CategoryEditRequestDTO;
import beyou.beyouapp.backend.domain.category.dto.CategoryRequestDTO;
import beyou.beyouapp.backend.domain.category.dto.CategoryResponseDTO;
import beyou.beyouapp.backend.domain.category.xpbylevel.XpByLevel;
import beyou.beyouapp.backend.domain.category.xpbylevel.XpByLevelRepository;
import beyou.beyouapp.backend.exceptions.category.CategoryNotFound;
import beyou.beyouapp.backend.exceptions.user.UserNotFound;
import beyou.beyouapp.backend.user.User;
import beyou.beyouapp.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private XpByLevelRepository xpByLevelRepository;

    @Autowired
    private UserRepository userRepository;

    public Category getCategory(UUID categoryId){
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFound("Category not found"));
    }

    public ArrayList<CategoryResponseDTO> getAllCategories(UUID userId){
        ArrayList<Category> categories = categoryRepository.findAllByUserId(userId)
                .orElseThrow(() -> new UserNotFound("User not found"));
        
        ArrayList<CategoryResponseDTO> categoryResponse = new ArrayList<>();
        List<Map<UUID, String>> habitIdAndName = new ArrayList<>();
        
        for(int i = 0; i < categories.size(); i++){
            int habitsInCategorySize = categories.get(i).getHabits().size();

            for(int j = 0; j < habitsInCategorySize; j++){
                UUID habitId = categories.get(i).getHabits().get(j).getId();
                String habitName = categories.get(i).getHabits().get(j).getName();
                habitIdAndName.add(Map.of(habitId, habitName));
            }

            CategoryResponseDTO categoryResponseDTO = new CategoryResponseDTO(categories.get(i).getId(), categories.get(i).getName(), 
            categories.get(i).getDescription(), categories.get(i).getIconId(), habitIdAndName, categories.get(i).getXp(), categories.get(i).getNextLevelXp(), categories.get(i).getActualLevelXp(), categories.get(i).getLevel(), categories.get(i).getCreatedAt());
            categoryResponse.add(categoryResponseDTO);
        }

        return categoryResponse;
    }

    public ResponseEntity<Map<String, Object>> createCategory(CategoryRequestDTO categoryRequestDTO){
        User user = userRepository.findById(UUID.fromString(categoryRequestDTO.userId()))
                .orElseThrow(() -> new UserNotFound("User not found"));

        XpByLevel xpForNextLevel = xpByLevelRepository.findByLevel(categoryRequestDTO.level() + 1);
        XpByLevel xpForActualLevel = xpByLevelRepository.findByLevel(categoryRequestDTO.level());

        Category newCategory = new Category(categoryRequestDTO, user);
        newCategory.setNextLevelXp(xpForNextLevel.getXp());
        newCategory.setActualLevelXp(xpForActualLevel.getXp());

        categoryRepository.save(newCategory);

        return ResponseEntity.ok().body(Map.of("success", newCategory));
    }

    public ResponseEntity<Map<String, Object>> editCategory(CategoryEditRequestDTO categoryEditRequestDTO){
        Category categoryToEdit = getCategory(UUID.fromString(categoryEditRequestDTO.categoryId()));

        categoryToEdit.setName(categoryEditRequestDTO.name());
        categoryToEdit.setDescription(categoryEditRequestDTO.description());
        categoryToEdit.setIconId(categoryEditRequestDTO.icon());

        categoryRepository.save(categoryToEdit);

        return ResponseEntity.ok().body(Map.of("success", categoryToEdit));
    }

    public ResponseEntity<Map<String, String>> deleteCategory(String categoryId){
        try{
            Category category = categoryRepository.findById(UUID.fromString(categoryId))
                    .orElseThrow(() -> new CategoryNotFound("Category not found"));

            categoryRepository.delete(category);
            return ResponseEntity.ok().body(Map.of("success", "Category deleted successfully"));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of("error", "errorTryingToDeleteCategory"));
        }
    }
}
