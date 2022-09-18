package hello.upload.controller;

import hello.upload.domain.Item;
import hello.upload.domain.ItemRepository;
import hello.upload.domain.UploadFile;
import hello.upload.file.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.channels.MulticastChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

  private final ItemRepository itemRepository;
  private final FileStore fileStore;

  @GetMapping("/items/new")
  public String newItem(@ModelAttribute ItemForm form) {
    return "item-form";
  }

  @PostMapping("/items/new")
  public String saveItem(@ModelAttribute ItemForm form, RedirectAttributes
      redirectAttributes) throws IOException {
    /*
    사용자에게서 받은 첨부 파일
    MultipartFile attachFile = form.getAttachFile();
    첨부 파일들 -> uploadFile로 변환
    UploadFile uploadFile = fileStore.storeFile(attachFile);

    List<MultipartFile> imageFiles = form.getImageFiles();
    List<UploadFile> uploadFiles = fileStore.storeFiles(imageFiles);
     */


    UploadFile attachFile = fileStore.storeFile(form.getAttachFile());
    List<UploadFile> storeImageFiles =
        fileStore.storeFiles(form.getImageFiles());
    //파일에 저장하는 행위 완료

    //데이터 베이스에 저장하자
    Item item = new Item();
    item.setItemName(form.getItemName());
    item.setAttachFile(attachFile);
    item.setImageFiles(storeImageFiles);
    itemRepository.save(item);

    redirectAttributes.addAttribute("itemId", item.getId());

    return "redirect:/items/{itemId}";
  }

  @GetMapping("/items/{id}")
  public String items(@PathVariable Long id, Model model) {
    Item item = itemRepository.findById(id);
    model.addAttribute("item", item);

    return "item-view";
  }

  @ResponseBody
  @GetMapping("/images/{filename}")
  public Resource downloadImage(@PathVariable String filename) throws
      MalformedURLException {
    return new UrlResource("file:" + fileStore.getFullPath(filename));
  }

  @GetMapping("/attach/{itemId}")
  public ResponseEntity<Resource> downloadAttach(@PathVariable Long itemId) throws MalformedURLException {
    Item item = itemRepository.findById(itemId);
    String storeFileName = item.getAttachFile().getStoreFileName();
    String uploadFileName = item.getAttachFile().getUploadFileName();

    UrlResource urlResource = new UrlResource("file:" + fileStore.getFullPath(storeFileName));

    log.info("uploadFileName = {}", uploadFileName);

    String encodingUploadFileName = UriUtils.encode(uploadFileName, StandardCharsets.UTF_8);
    String contentDisposition = "attachment; filename=\"" + encodingUploadFileName + "\"";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .body(urlResource);
  }

}
