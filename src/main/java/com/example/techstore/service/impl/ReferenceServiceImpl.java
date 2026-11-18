package com.example.techstore.service.impl;

import com.example.techstore.dto.ReferenceDtos.BrandDto;
import com.example.techstore.dto.ReferenceDtos.CategoryDto;
import com.example.techstore.dto.ReferenceDtos.CharacteristicDto;
import com.example.techstore.exception.ConflictException;
import com.example.techstore.exception.ErrorCode;
import com.example.techstore.exception.InvalidCharacteristicException;
import com.example.techstore.exception.NotFoundException;
import com.example.techstore.model.catalog.CategoryCharacteristic;
import com.example.techstore.model.catalog.CategoryCharacteristicId;
import com.example.techstore.model.catalog.Characteristic;
import com.example.techstore.repository.BrandRepository;
import com.example.techstore.repository.CategoryCharacteristicRepository;
import com.example.techstore.repository.CategoryRepository;
import com.example.techstore.repository.CharacteristicRepository;
import com.example.techstore.repository.ProductRepository;
import com.example.techstore.service.ProductRules;
import com.example.techstore.service.ReferenceService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReferenceServiceImpl implements ReferenceService {
    private final BrandRepository brands;
    private final CategoryRepository categories;
    private final CharacteristicRepository specifications;
    private final CategoryCharacteristicRepository links;
    private final ProductRepository products;

    public ReferenceServiceImpl(
            BrandRepository brands,
            CategoryRepository categories,
            CharacteristicRepository specifications,
            CategoryCharacteristicRepository links,
            ProductRepository products) {
        this.brands = brands;
        this.categories = categories;
        this.specifications = specifications;
        this.links = links;
        this.products = products;
    }

    @Override
    public List<BrandDto> brands() {
        return brands.findAll().stream().map(b -> new BrandDto(b.getName())).toList();
    }

    @Override
    public BrandDto brand(String name) {
        return brands.findById(name)
                .map(brand -> new BrandDto(brand.getName()))
                .orElseThrow(() -> new NotFoundException(ErrorCode.BRAND_NOT_FOUND, "Бренд не найден: " + name));
    }

    @Transactional
    @Override
    public BrandDto createBrand(BrandDto request) {
        String name = request.name().strip();
        if (brands.existsById(name))
            throw new ConflictException(ErrorCode.DUPLICATE_BRAND, "Бренд уже существует");
        brands.saveAndFlush(new com.example.techstore.model.catalog.Brand(name));
        return new BrandDto(name);
    }

    @Transactional
    @Override
    public BrandDto renameBrand(String old, BrandDto request) {
        if (!brands.existsById(old))
            throw new NotFoundException(ErrorCode.BRAND_NOT_FOUND, "Бренд не найден");
        String name = request.name().strip();
        if (old.equals(name)) return new BrandDto(name);
        if (products.existsByBrandName(old))
            throw new ConflictException(
                    ErrorCode.BRAND_IN_USE,
                    "Нельзя переименовать используемый бренд");
        BrandDto result = createBrand(request);
        brands.deleteById(old);
        brands.flush();
        return result;
    }

    @Transactional
    @Override
    public void deleteBrand(String name) {
        if (!brands.existsById(name))
            throw new NotFoundException(ErrorCode.BRAND_NOT_FOUND, "Бренд не найден");
        if (products.existsByBrandName(name))
            throw new ConflictException(ErrorCode.BRAND_IN_USE, "Бренд используется товарами");
        brands.deleteById(name);
        brands.flush();
    }

    @Override
    public List<CategoryDto> categories() {
        return categories.findAll().stream()
                .map(c -> new CategoryDto(c.getCode(), c.getName()))
                .toList();
    }

    @Override
    public List<CharacteristicDto> specifications() {
        return specifications.findAll().stream().map(this::dto).toList();
    }

    @Override
    public CharacteristicDto specification(String name) {
        return dto(requireSpecification(name));
    }

    @Transactional
    @Override
    public CharacteristicDto createSpecification(CharacteristicDto request) {
        String name = request.name().strip();
        if (specifications.existsById(name))
            throw new ConflictException(
                    ErrorCode.DUPLICATE_CHARACTERISTIC, "Характеристика уже существует: " + name);
        Characteristic entity = new Characteristic();
        entity.setName(name);
        assign(entity, request);
        specifications.saveAndFlush(entity);
        return dto(entity);
    }

    @Transactional
    @Override
    public CharacteristicDto updateSpecification(String name, CharacteristicDto request) {
        Characteristic entity = requireSpecification(name);
        if (!name.equals(request.name().strip()))
            throw new InvalidCharacteristicException(
                    ErrorCode.CHARACTERISTIC_NAME_IMMUTABLE, "Имя характеристики нельзя изменить: " + name);
        if (!entity.getValueType().equals(request.valueType())
                && (products.existsBySpecificationsIdSpecificationName(name)
                        || links.existsByIdSpecificationName(name)))
            throw new ConflictException(
                    ErrorCode.CHARACTERISTIC_IN_USE, "Тип используемой характеристики менять нельзя: " + name);
        assign(entity, request);
        specifications.flush();
        return dto(entity);
    }

    @Transactional
    @Override
    public void deleteSpecification(String name) {
        Characteristic entity = requireSpecification(name);
        if (links.existsByIdSpecificationName(name)
                || products.existsBySpecificationsIdSpecificationName(name))
            throw new ConflictException(
                    ErrorCode.CHARACTERISTIC_IN_USE, "Характеристика используется: " + name);
        specifications.delete(entity);
        specifications.flush();
    }

    @Override
    public List<String> categorySpecifications(String code) {
        requireCategory(code);
        return links.findByIdCategoryCode(code).stream()
                .map(e -> e.getId().getSpecificationName())
                .sorted()
                .toList();
    }

    @Transactional
    @Override
    public void link(String code, String name) {
        requireCategory(code);
        requireSpecification(name);
        CategoryCharacteristicId id = new CategoryCharacteristicId(code, name);
        if (!links.existsById(id)) links.saveAndFlush(new CategoryCharacteristic(code, name));
    }

    @Transactional
    @Override
    public void unlink(String code, String name) {
        requireCategory(code);
        CategoryCharacteristicId id = new CategoryCharacteristicId(code, name);
        if (!links.existsById(id))
            throw new NotFoundException(
                    ErrorCode.CHARACTERISTIC_LINK_NOT_FOUND, "Связь не найдена: " + code + " / " + name);
        if (ProductRules.required(com.example.techstore.model.product.ProductCategory.valueOf(code))
                .contains(name))
            throw new ConflictException(
                    ErrorCode.CHARACTERISTIC_REQUIRED, "Характеристика обязательна для категории " + code + ": " + name);
        if (products.existsBySpecificationsIdSpecificationName(name))
            throw new ConflictException(
                    ErrorCode.CHARACTERISTIC_IN_USE, "Характеристика используется товарами: " + name);
        links.deleteById(id);
        links.flush();
    }

    private void requireCategory(String code) {
        if (!categories.existsById(code))
            throw new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND, "Категория не найдена");
    }

    private Characteristic requireSpecification(String name) {
        return specifications
                .findById(name)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CHARACTERISTIC_NOT_FOUND, "Характеристика не найдена: " + name));
    }

    private CharacteristicDto dto(Characteristic entity) {
        return new CharacteristicDto(entity.getName(), entity.getValueType(), entity.getUnit());
    }

    private void assign(Characteristic entity, CharacteristicDto request) {
        if (request.unit() != null && request.unit().isBlank())
            throw new InvalidCharacteristicException(
                    ErrorCode.INVALID_CHARACTERISTIC_UNIT, "Единица измерения не должна быть пустой");
        entity.setValueType(request.valueType());
        entity.setUnit(request.unit() == null ? null : request.unit().strip());
    }
}
