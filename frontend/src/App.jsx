import { Navigate, Route, Routes } from "react-router";

import AppLayout from "./layouts/AppLayout";
import LoginPage from "./pages/LoginPage";
import QuizPage from "./pages/QuizPage";
import SignupPage from "./pages/SignupPage";
import DashboardPage from "./pages/lab/DashboardPage";
import ProblemCreatePage from "./pages/lab/ProblemCreatePage";
import ProblemListPage from "./pages/lab/ProblemListPage";
import ProblemWorkspacePage from "./pages/lab/ProblemWorkspacePage";
import ProfilePage from "./pages/lab/ProfilePage";
import StatisticsPage from "./pages/lab/StatisticsPage";
import WrongNotesPage from "./pages/lab/WrongNotesPage";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      <Route element={<AppLayout />}>
        <Route path="/home" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/problems" element={<ProblemListPage />} />
        <Route path="/problems/new" element={<ProblemCreatePage />} />
        <Route path="/quiz" element={<QuizPage />} />
        <Route
          path="/problems/:problemId"
          element={<ProblemWorkspacePage />}
        />
        <Route path="/wrong-notes" element={<WrongNotesPage />} />
        <Route path="/statistics" element={<StatisticsPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
