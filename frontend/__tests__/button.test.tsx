import { describe, it, expect } from "vitest"
import { render, screen } from "@testing-library/react"
import { Button } from "@/components/ui/button"

describe("Button", () => {
  it("renders its children as the button label", () => {
    render(<Button>Save changes</Button>)
    expect(
      screen.getByRole("button", { name: /save changes/i })
    ).toBeInTheDocument()
  })

  it("applies the variant via data attribute", () => {
    render(<Button variant="destructive">Delete</Button>)
    expect(
      screen.getByRole("button", { name: /delete/i })
    ).toHaveAttribute("data-variant", "destructive")
  })
})
