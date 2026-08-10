package com.trivocab.ielts.service;

import com.trivocab.ielts.domain.UserBookSettingsRow;
import com.trivocab.ielts.domain.WordBookRow;
import com.trivocab.ielts.dto.BookSelectionItem;
import com.trivocab.ielts.dto.BookSelectionResponse;
import com.trivocab.ielts.exception.ResourceNotFoundException;
import com.trivocab.ielts.mapper.UserBookMapper;
import com.trivocab.ielts.mapper.WordBookMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class BookSelectionService {
    private final WordBookMapper wordBookMapper;
    private final UserBookMapper userBookMapper;
    private final Clock clock;
    private final ZoneId applicationZoneId;

    public BookSelectionService(
            WordBookMapper wordBookMapper,
            UserBookMapper userBookMapper,
            Clock clock,
            ZoneId applicationZoneId
    ) {
        this.wordBookMapper = wordBookMapper;
        this.userBookMapper = userBookMapper;
        this.clock = clock;
        this.applicationZoneId = applicationZoneId;
    }

    public BookSelectionResponse selection(long userId) {
        List<WordBookRow> books = wordBookMapper.findAll(userId);
        Long selectedBookId = userBookMapper.findSelectedBookId(userId);
        int defaultDailyGoal = userBookMapper.findDefaultDailyGoal(userId);
        LocalDate today = LocalDate.now(applicationZoneId);
        Long resolvedSelected = selectedBookId;
        if (resolvedSelected == null) {
            resolvedSelected = books.isEmpty() ? null : books.get(0).getId();
        }
        final Long effectiveSelected = resolvedSelected;

        List<BookSelectionItem> items = books.stream().map(book -> {
            int total = value(book.getTotalWords());
            int learned = value(book.getLearnedWords());
            int dailyGoal = settingDailyGoal(userId, book.getId(), defaultDailyGoal);
            int remaining = Math.max(0, total - learned);
            int estimatedDays = estimateDays(remaining, dailyGoal);
            LocalDate completionDate = estimatedDays == 0
                    ? today
                    : today.plusDays(estimatedDays - 1L);
            double percent = total == 0 ? 0.0 : Math.round(learned * 1000.0 / total) / 10.0;
            return new BookSelectionItem(
                    book.getId(),
                    book.getCode(),
                    book.getName(),
                    book.getDescription(),
                    total,
                    learned,
                    percent,
                    dailyGoal,
                    remaining,
                    estimatedDays,
                    completionDate,
                    book.getId().equals(effectiveSelected)
            );
        }).toList();

        return new BookSelectionResponse(effectiveSelected, defaultDailyGoal, items);
    }

    @Transactional
    public BookSelectionResponse switchBook(long userId, long bookId) {
        WordBookRow book = wordBookMapper.findById(bookId, userId);
        if (book == null) {
            throw new ResourceNotFoundException("词书不存在");
        }
        if (userBookMapper.findSetting(userId, bookId) == null) {
            UserBookSettingsRow setting = new UserBookSettingsRow();
            setting.setUserId(userId);
            setting.setBookId(bookId);
            setting.setDailyGoal(userBookMapper.findDefaultDailyGoal(userId));
            userBookMapper.insertSetting(setting);
        }
        userBookMapper.updateSelectedBook(userId, bookId);
        return selection(userId);
    }

    private int settingDailyGoal(long userId, long bookId, int fallback) {
        UserBookSettingsRow setting = userBookMapper.findSetting(userId, bookId);
        if (setting == null || setting.getDailyGoal() == null) {
            return fallback;
        }
        return setting.getDailyGoal();
    }

    private int estimateDays(int remainingWords, int dailyGoal) {
        if (remainingWords <= 0) {
            return 0;
        }
        if (dailyGoal <= 0) {
            return 0;
        }
        return (remainingWords + dailyGoal - 1) / dailyGoal;
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }
}
