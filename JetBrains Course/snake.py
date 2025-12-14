"""
Simple Snake game implemented in Python with pygame.

How to run (Windows/macOS/Linux):
1) Install Python 3.9+ and pip.
2) Install dependency: pip install pygame>=2.5
3) From this folder, run: python snake.py

Controls:
- Arrow keys or WASD to move
- Space to pause/resume
- R to restart
- Esc or window close to quit

Gameplay mirrors the HTML/JS version in index.html:
- 25x25 grid, 24px cells (600x600 window)
- Food spawns avoiding the snake body
- Score and best score (saved to snake_best.txt next to this file)
- Speed increases slightly after each food
"""

from __future__ import annotations

import os
import random
import sys
from pathlib import Path
from typing import Deque, List, Tuple

import pygame


# -------------------- Config --------------------
GRID_SIZE = 25  # cells per row/column
CELL = 24       # pixel size of each cell (25 * 24 = 600)
START_LEN = 4
BASE_TPS = 8.0  # ticks per second base
SPEED_STEP = 0.05  # speed increase per food
MAX_SPEED_MULT = 2.0

WINDOW_SIZE = GRID_SIZE * CELL
CAPTION = "Simple Snake (Python)"


# Colors (approximate those used in the HTML version)
BG = (15, 18, 32)            # --bg
PANEL = (21, 26, 46)         # --panel
TEXT = (223, 231, 255)       # --text
GRID = (31, 39, 71)          # --grid
SNAKE = (83, 242, 139)       # --snake
SNAKE_HEAD = (52, 212, 107)  # --snake-head
FOOD = (255, 200, 87)        # --food

HILIGHT = (255, 255, 255, 20)  # subtle bevel highlight (alpha)
SHADOW = (0, 0, 0, 38)         # subtle bevel shadow (alpha)


class Game:
    def __init__(self) -> None:
        pygame.init()
        pygame.display.set_caption(CAPTION)
        self.screen = pygame.display.set_mode((WINDOW_SIZE, WINDOW_SIZE))
        self.clock = pygame.time.Clock()
        self.font = pygame.font.SysFont(None, 22)
        self.big_font = pygame.font.SysFont(None, 36)

        # Pre-render grid background
        self.board_surface = pygame.Surface((WINDOW_SIZE, WINDOW_SIZE))
        self.board_surface.fill(PANEL)
        self._draw_grid(self.board_surface)

        # State
        self.snake: List[Tuple[int, int]] = []
        self.dir: Tuple[int, int] = (1, 0)
        self.next_dir_queue: List[Tuple[int, int]] = []
        self.food: Tuple[int, int] = (0, 0)
        self.score: int = 0
        self.best: int = 0
        self.speed_mult: float = 1.0
        self.paused: bool = False
        self.game_over: bool = False
        self.tick_accum: float = 0.0

        self.best = self._load_best()
        self._init_game()

    # -------------------- Persistence --------------------
    def _best_path(self) -> Path:
        return Path(__file__).with_name("snake_best.txt")

    def _load_best(self) -> int:
        try:
            p = self._best_path()
            if p.exists():
                return int(p.read_text(encoding="utf-8").strip() or "0")
        except Exception:
            pass
        return 0

    def _save_best(self) -> None:
        try:
            self._best_path().write_text(str(self.best), encoding="utf-8")
        except Exception:
            # ignore I/O errors silently
            pass

    # -------------------- Game Setup --------------------
    def _init_game(self) -> None:
        cx = GRID_SIZE // 2
        cy = GRID_SIZE // 2
        self.snake = [(cx - i, cy) for i in range(START_LEN)]
        self.dir = (1, 0)
        self.next_dir_queue = []
        self.food = self._spawn_food()
        self.score = 0
        self.speed_mult = 1.0
        self.paused = False
        self.game_over = False
        self.tick_accum = 0.0

    def _spawn_food(self) -> Tuple[int, int]:
        occupied = {f"{x},{y}" for (x, y) in self.snake}
        while True:
            x = random.randrange(GRID_SIZE)
            y = random.randrange(GRID_SIZE)
            if f"{x},{y}" not in occupied:
                return (x, y)

    # -------------------- Input --------------------
    def _handle_event(self, e: pygame.event.Event) -> None:
        if e.type == pygame.QUIT:
            self._quit()
        elif e.type == pygame.KEYDOWN:
            if e.key == pygame.K_ESCAPE:
                self._quit()
                return
            if e.key == pygame.K_SPACE:
                if not self.game_over:
                    self.paused = not self.paused
                return
            if e.key == pygame.K_r:  # restart
                self._init_game()
                return

            nd = None
            if e.key in (pygame.K_UP, pygame.K_w):
                nd = (0, -1)
            elif e.key in (pygame.K_DOWN, pygame.K_s):
                nd = (0, 1)
            elif e.key in (pygame.K_LEFT, pygame.K_a):
                nd = (-1, 0)
            elif e.key in (pygame.K_RIGHT, pygame.K_d):
                nd = (1, 0)

            if nd is not None:
                # Enqueue at most one pending change; replace if too many
                if not self.next_dir_queue:
                    self.next_dir_queue.append(nd)
                else:
                    self.next_dir_queue[-1] = nd

    # -------------------- Game Loop --------------------
    def run(self) -> None:
        while True:
            dt_ms = self.clock.tick(120)  # render at up to 120 FPS
            for e in pygame.event.get():
                self._handle_event(e)

            # Timing for ticks
            if not (self.paused or self.game_over):
                tps = BASE_TPS * min(self.speed_mult, MAX_SPEED_MULT)
                tick_interval = max(0.03, 1.0 / tps)  # clamp ~30ms min
                self.tick_accum += dt_ms / 1000.0
                while self.tick_accum >= tick_interval:
                    self.tick_accum -= tick_interval
                    self._tick()

            # Render
            self._draw()

    def _tick(self) -> None:
        # Consume pending direction change (one per tick)
        if self.next_dir_queue:
            nd = self.next_dir_queue.pop(0)
            # Prevent reversing directly into the body
            if not (nd[0] == -self.dir[0] and nd[1] == -self.dir[1]):
                self.dir = nd

        head_x, head_y = self.snake[0]
        nx = head_x + self.dir[0]
        ny = head_y + self.dir[1]

        # Collisions: walls
        if nx < 0 or nx >= GRID_SIZE or ny < 0 or ny >= GRID_SIZE:
            self._end_game()
            return

        # Collisions: self
        if (nx, ny) in self.snake:
            self._end_game()
            return

        # Move
        self.snake.insert(0, (nx, ny))

        # Eat food?
        if (nx, ny) == self.food:
            self.score += 1
            if self.score > self.best:
                self.best = self.score
                self._save_best()
            self.speed_mult += SPEED_STEP
            self.food = self._spawn_food()
        else:
            self.snake.pop()  # remove tail

    def _end_game(self) -> None:
        self.game_over = True

    # -------------------- Rendering --------------------
    def _draw_grid(self, surface: pygame.Surface) -> None:
        # Subtle grid lines
        for x in range(0, WINDOW_SIZE + 1, CELL):
            pygame.draw.line(surface, GRID, (x, 0), (x, WINDOW_SIZE), 1)
        for y in range(0, WINDOW_SIZE + 1, CELL):
            pygame.draw.line(surface, GRID, (0, y), (WINDOW_SIZE, y), 1)

    def _draw_cell(self, surface: pygame.Surface, x: int, y: int, color: Tuple[int, int, int]) -> None:
        px = x * CELL
        py = y * CELL
        # main square with small inset to create border feel
        rect = pygame.Rect(px + 1, py + 1, CELL - 2, CELL - 2)
        pygame.draw.rect(surface, color, rect)

        # subtle bevel using alpha surfaces
        hl = pygame.Surface((CELL - 2, 2), pygame.SRCALPHA)
        hl.fill(HILIGHT)
        surface.blit(hl, (px + 1, py + 1))  # top
        hl2 = pygame.Surface((2, CELL - 2), pygame.SRCALPHA)
        hl2.fill(HILIGHT)
        surface.blit(hl2, (px + 1, py + 1))  # left

        sh = pygame.Surface((CELL - 2, 2), pygame.SRCALPHA)
        sh.fill(SHADOW)
        surface.blit(sh, (px + 1, py + CELL - 3))  # bottom
        sh2 = pygame.Surface((2, CELL - 2), pygame.SRCALPHA)
        sh2.fill(SHADOW)
        surface.blit(sh2, (px + CELL - 3, py + 1))  # right

    def _draw(self) -> None:
        # Background board
        self.screen.fill(BG)
        self.screen.blit(self.board_surface, (0, 0))

        # Food
        self._draw_cell(self.screen, self.food[0], self.food[1], FOOD)

        # Snake (tail to head for bevel overlap similar to JS)
        for i in range(len(self.snake) - 1, -1, -1):
            x, y = self.snake[i]
            if i == 0:
                self._draw_cell(self.screen, x, y, SNAKE_HEAD)
                # eyes
                cx = x * CELL + CELL // 2
                cy = y * CELL + CELL // 2
                eye_offset = 5
                eye_radius = 2
                eye_dx = (6 if self.dir[0] != 0 else 0) * (1 if self.dir[0] >= 0 else -1)
                eye_dy = (6 if self.dir[1] != 0 else 0) * (1 if self.dir[1] >= 0 else -1)
                if self.dir[0] != 0:
                    pygame.draw.circle(self.screen, (0, 0, 0, 128), (cx + (6 if self.dir[0] > 0 else -6), cy - eye_offset), eye_radius)
                    pygame.draw.circle(self.screen, (0, 0, 0, 128), (cx + (6 if self.dir[0] > 0 else -6), cy + eye_offset), eye_radius)
                else:
                    pygame.draw.circle(self.screen, (0, 0, 0, 128), (cx - eye_offset, cy + (6 if self.dir[1] > 0 else -6)), eye_radius)
                    pygame.draw.circle(self.screen, (0, 0, 0, 128), (cx + eye_offset, cy + (6 if self.dir[1] > 0 else -6)), eye_radius)
            else:
                self._draw_cell(self.screen, x, y, SNAKE)

        # HUD (top left)
        speed_text = f"{self.speed_mult:.1f}x"
        hud = [
            (f"Score: {self.score}", 10),
            (f"Best: {self.best}", 120),
            (f"Speed: {speed_text}", 220),
        ]
        for text, x in hud:
            surf = self.font.render(text, True, TEXT)
            self.screen.blit(surf, (x, 8))

        # Overlays
        if self.paused:
            self._draw_overlay("Paused", "Press Space to resume")
        elif self.game_over:
            self._draw_overlay("Game Over", f"Score: {self.score}  ·  Press R to restart")

        pygame.display.flip()

    def _draw_overlay(self, title: str, subtitle: str) -> None:
        overlay = pygame.Surface((WINDOW_SIZE, WINDOW_SIZE), pygame.SRCALPHA)
        overlay.fill((0, 0, 0, 128))
        self.screen.blit(overlay, (0, 0))

        title_surf = self.big_font.render(title, True, TEXT)
        sub_surf = self.font.render(subtitle, True, TEXT)
        rect = title_surf.get_rect(center=(WINDOW_SIZE // 2, WINDOW_SIZE // 2 - 16))
        self.screen.blit(title_surf, rect)
        rect2 = sub_surf.get_rect(center=(WINDOW_SIZE // 2, WINDOW_SIZE // 2 + 16))
        self.screen.blit(sub_surf, rect2)

    # -------------------- Misc --------------------
    @staticmethod
    def _quit() -> None:
        pygame.quit()
        sys.exit(0)


def main() -> None:
    game = Game()
    game.run()


if __name__ == "__main__":
    main()
