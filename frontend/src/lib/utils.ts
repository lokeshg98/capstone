import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/** Combines clsx + tailwind-merge so conditional class strings don't conflict. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
