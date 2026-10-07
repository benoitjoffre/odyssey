import { apiFetch } from "./client";

export type ImageFolder = "experiences" | "travel-events";

interface ImageUploadResponse {
  imageUrl: string;
}

export function uploadImage(file: File, folder: ImageFolder): Promise<ImageUploadResponse> {
  const formData = new FormData();
  formData.append("file", file);

  return apiFetch<ImageUploadResponse>(`/api/images/${folder}/upload`, {
    method: "POST",
    body: formData,
  });
}
